package mpdc4gsr.feature.capture.thermal.data

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.logging.StructuredLogger
import mpdc4gsr.feature.capture.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.capture.thermal.domain.usecase.ThermalCoreUseCases
import mpdc4gsr.feature.capture.thermal.domain.usecase.ThermalHardwareUseCases
import mpdc4gsr.feature.connectivity.data.DataManagementService

private const val DEFAULT_DEVICE_ID = "android_thermal"

@Singleton
class ThermalCaptureCoordinator
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val thermalUseCases: ThermalCoreUseCases,
        @Suppress("UnusedPrivateMember")
        private val hardwareUseCases: ThermalHardwareUseCases,
        private val dataManagementService: DataManagementService,
    ) {
        private val logger = StructuredLogger.getInstance(appContext)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val status = MutableStateFlow(ThermalStatus())
        private val preview = MutableStateFlow<ThermalFrameData?>(null)
        private val errors = MutableSharedFlow<String>(extraBufferCapacity = 8)

        private val sessionMutex = Mutex()
        private val streamingMutex = Mutex()

        private val connected = AtomicBoolean(false)
        private val recording = AtomicBoolean(false)

        private var streamingJob: Job? = null
        private var activeSession: ThermalSessionContext? = null
        private var lastRecordingResult: ThermalRecordingResult? = null

        fun statusFlow(): StateFlow<ThermalStatus> = status.asStateFlow()

        fun previewFlow(): StateFlow<ThermalFrameData?> = preview.asStateFlow()

        fun errorFlow(): SharedFlow<String> = errors.asSharedFlow()

        suspend fun ensureConnected(): Boolean {
            if (connected.get()) return true
            val result = withContext(Dispatchers.IO) { thermalUseCases.connectCamera() }
            return if (result.isSuccess) {
                connected.set(true)
                updateStatus { copy(isConnected = true, lastError = null) }
                startStreamingIfNeeded()
                true
            } else {
                val throwable = result.exceptionOrNull()
                emitError("Unable to connect to thermal camera: ${throwable?.message ?: "unknown"}")
                updateStatus { copy(isConnected = false, isStreaming = false) }
                false
            }
        }

        suspend fun startSession(context: ThermalSessionContext): Boolean =
            sessionMutex.withLock {
                if (recording.get()) {
                    emitError("Thermal session already active")
                    return@withLock false
                }
                if (!ensureConnected()) {
                    emitError("Thermal camera not connected; cannot start session ${context.sessionId}")
                    return@withLock false
                }

                val sessionDir = File(context.sessionDirectory, "Thermal").apply { mkdirs() }
                val sessionContext = context.copy(sessionDirectory = sessionDir)
                activeSession = sessionContext

                val metadata =
                    buildMap<String, Any?> {
                        put("trigger_source", sessionContext.triggerSource)
                        putAll(sessionContext.metadata)
                    }
                runCatching {
                    ensureDataManagementSession(sessionContext.sessionId, metadata)
                }.onFailure { throwable ->
                    emitError("Failed to register thermal session metadata: ${throwable.message}")
                }

                val startResult = withContext(Dispatchers.IO) { thermalUseCases.startRecording() }
                val started =
                    if (startResult.isSuccess) {
                        recording.set(true)
                        updateStatus {
                            copy(
                                isRecording = true,
                                recordingStartElapsedMs = SystemClock.elapsedRealtime(),
                                lastError = null,
                            )
                        }
                        true
                    } else {
                        val throwable = startResult.exceptionOrNull()
                        emitError("Failed to start thermal recording: ${throwable?.message ?: "unknown"}")
                        false
                    }
                if (started) {
                    startStreamingIfNeeded()
                }
                started
            }

        suspend fun stopSession(): ThermalRecordingResult? =
            sessionMutex.withLock {
                if (!recording.get()) return@withLock null
                val session = activeSession
                val stopResult = withContext(Dispatchers.IO) { thermalUseCases.stopRecording() }
                val result =
                    if (stopResult.isSuccess) {
                        val path = stopResult.getOrNull()
                        val recordingFile =
                            path?.takeIf { it.isNotBlank() }?.let { File(it) }
                                ?: createPlaceholderRecording(session)
                        finalizeRecording(session, recordingFile)
                    } else {
                        val throwable = stopResult.exceptionOrNull()
                        emitError("Failed to stop thermal recording: ${throwable?.message ?: "unknown"}")
                        null
                    }
                recording.set(false)
                updateStatus {
                    copy(
                        isRecording = false,
                        recordingDurationMs = 0L,
                        lastRecordingPath = result?.file?.absolutePath,
                        lastRecordingMetadata = result?.metadata ?: emptyMap(),
                    )
                }
                activeSession = null
                lastRecordingResult = result
                result
            }

        fun lastRecordingResult(): ThermalRecordingResult? = lastRecordingResult

        fun shutdown() {
            streamingJob?.cancel()
            streamingJob = null
            scope.coroutineContext.cancel()
            connected.set(false)
            recording.set(false)
            activeSession = null
        }

        private suspend fun startStreamingIfNeeded() {
            streamingMutex.withLock {
                if (streamingJob?.isActive == true) return
                streamingJob =
                    scope.launch {
                        try {
                            val stream = thermalUseCases.startStreaming()
                            stream.collect { frame ->
                                preview.value = frame
                                status.update { current ->
                                    val newMin = current.minTemperature?.let { min(it, frame.minTemp) } ?: frame.minTemp
                                    val newMax = current.maxTemperature?.let { max(it, frame.maxTemp) } ?: frame.maxTemp
                                    val duration =
                                        if (recording.get() && current.recordingStartElapsedMs > 0L) {
                                            SystemClock.elapsedRealtime() - current.recordingStartElapsedMs
                                        } else {
                                            current.recordingDurationMs
                                        }
                                    current.copy(
                                        isStreaming = true,
                                        frameCount = current.frameCount + 1,
                                        currentTemperature = frame.centerTemp,
                                        minTemperature = newMin,
                                        maxTemperature = newMax,
                                        recordingDurationMs = duration,
                                        lastFrameTimestampMs = frame.timestamp,
                                    )
                                }
                            }
                        } catch (throwable: Throwable) {
                            emitError("Thermal streaming stopped: ${throwable.message ?: "unknown"}")
                            updateStatus { copy(isStreaming = false) }
                        }
                    }
            }
        }

        private fun ensureDataManagementSession(
            sessionId: String,
            metadata: Map<String, Any?>,
        ) {
            val existing = dataManagementService.getSession(sessionId)
            if (existing == null) {
                dataManagementService.createSession(
                    sessionId = sessionId,
                    deviceId = DEFAULT_DEVICE_ID,
                    customMetadata = metadata,
                )
            } else {
                existing.metadata.putAll(metadata)
            }
        }

        private fun finalizeRecording(
            session: ThermalSessionContext?,
            recordingFile: File,
        ): ThermalRecordingResult? {
            val context = session ?: return null
            val metadata =
                mapOf(
                    "simulation" to false,
                    "trigger_source" to context.triggerSource,
                    "duration_ms" to status.value.recordingDurationMs,
                    "min_temp" to status.value.minTemperature,
                    "max_temp" to status.value.maxTemperature,
                )
            dataManagementService.registerFile(
                filePath = recordingFile.absolutePath,
                sessionId = context.sessionId,
                deviceId = DEFAULT_DEVICE_ID,
                fileType = "thermal_recording",
                customMetadata = metadata,
            )
            return ThermalRecordingResult(
                sessionId = context.sessionId,
                file = recordingFile,
                manifest = null,
                metadata = metadata,
                framesRecorded = status.value.frameCount,
                minTemp = status.value.minTemperature,
                maxTemp = status.value.maxTemperature,
                simulation = false,
            )
        }

        private fun createPlaceholderRecording(session: ThermalSessionContext?): File {
            val directory = session?.sessionDirectory ?: appContext.cacheDir
            val file = File(directory, "thermal_placeholder.txt")
            file.parentFile?.mkdirs()
            file.writeText("Thermal recording placeholder generated at ${System.currentTimeMillis()}")
            return file
        }

        private suspend fun emitError(message: String) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "ThermalCaptureCoordinator",
                "thermal_error",
                mapOf("message" to message),
            )
            errors.emit(message)
            updateStatus { copy(lastError = message) }
        }

        private fun updateStatus(block: ThermalStatus.() -> ThermalStatus) {
            status.update(block)
        }
    }

data class ThermalSessionContext(
    val sessionId: String,
    val sessionDirectory: File,
    val triggerSource: String,
    val metadata: Map<String, Any?> = emptyMap(),
)

data class ThermalRecordingResult(
    val sessionId: String,
    val file: File,
    val manifest: File?,
    val metadata: Map<String, Any?>,
    val framesRecorded: Long,
    val minTemp: Float?,
    val maxTemp: Float?,
    val simulation: Boolean,
)

data class ThermalStatus(
    val isConnected: Boolean = false,
    val isStreaming: Boolean = false,
    val isRecording: Boolean = false,
    val isSimulation: Boolean = false,
    val frameCount: Long = 0L,
    val currentTemperature: Float? = null,
    val minTemperature: Float? = null,
    val maxTemperature: Float? = null,
    val recordingStartElapsedMs: Long = 0L,
    val recordingDurationMs: Long = 0L,
    val lastFrameTimestampMs: Long? = null,
    val lastRecordingPath: String? = null,
    val lastRecordingMetadata: Map<String, Any?> = emptyMap(),
    val lastError: String? = null,
    val batteryPercent: Int? = null,
    val ispActive: Boolean = false,
    val frameDropCount: Int = 0,
)
