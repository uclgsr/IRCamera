package mpdc4gsr.feature.capture.thermal.data

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

private const val BATTERY_POLL_INTERVAL_MS = 30_000L
private const val TARGET_FRAME_RATE_HZ = 9.0

/**
 * Coordinates thermal camera connectivity, streaming, recording, and session bookkeeping.
 *
 * The coordinator keeps a single streaming job alive once a connection is established so
 * downstream consumers (UI preview, network preview, statistics) receive continuous data.
 * Recording sessions are reference-counted via [startSession] / [stopSession]; output files
 * are moved into the caller-provided session directory and registered with
 * [DataManagementService] for later aggregation.
 */
@Singleton
class ThermalCaptureCoordinator
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val thermalUseCases: ThermalCoreUseCases,
        private val hardwareUseCases: ThermalHardwareUseCases,
        private val dataManagementService: DataManagementService,
    ) {
        private val logger = StructuredLogger.getInstance(appContext)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val streamingJobMutex = Mutex()
        private val sessionMutex = Mutex()

        private val _status = MutableStateFlow(ThermalStatus())
        private val _preview = MutableStateFlow<ThermalFrameData?>(null)
        private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)

    private var streamingJob: Job? = null
    private val isConnected = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    private var batteryPollJob: Job? = null

    private var recordingStartElapsedMs: Long = 0L
    private var framesRecorded: Long = 0L
    private var sessionMinTemp: Float = Float.POSITIVE_INFINITY
    private var sessionMaxTemp: Float = Float.NEGATIVE_INFINITY
        private var activeSession: ThermalSessionContext? = null
        private var lastRecordingResult: ThermalRecordingResult? = null

        init {
            // Ensure storage directories exist on first use.
            runCatching { dataManagementService.initialize() }
        }

        fun statusFlow(): StateFlow<ThermalStatus> = _status.asStateFlow()

        fun previewFlow(): StateFlow<ThermalFrameData?> = _preview.asStateFlow()

        fun errorFlow(): SharedFlow<String> = _errors.asSharedFlow()

        suspend fun ensureConnected(): Boolean {
            if (isConnected.get()) {
                startStreamingIfNeeded()
                startBatteryMonitoring()
                return true
            }

            val result =
                withContext(Dispatchers.IO) {
                    thermalUseCases.connectCamera()
                }
            return result
                .onSuccess {
                    isConnected.set(true)
                    updateStatus {
                        copy(
                            isConnected = true,
                            isSimulation = thermalUseCases.isSimulationMode(),
                        )
                    }
                    startStreamingIfNeeded()
                    startBatteryMonitoring()
                }.onFailure { throwable ->
                    emitError("Unable to connect to thermal camera: ${throwable.message ?: "unknown error"}")
                    updateStatus { copy(isConnected = false, isStreaming = false) }
                }.isSuccess
        }

        suspend fun startSession(context: ThermalSessionContext): Boolean =
            sessionMutex.withLock {
                if (isRecording.get()) {
                    emitError("Thermal session already active for ${activeSession?.sessionId}")
                    return@withLock false
                }

                if (!ensureConnected()) {
                    emitError("Thermal camera not connected; cannot start session ${context.sessionId}")
                    return@withLock false
                }

                startStreamingIfNeeded()

                val sanitizedDir =
                    File(context.sessionDirectory, "Thermal").apply {
                        if (!exists()) {
                            mkdirs()
                        }
                    }

                val metadata =
                    buildMap {
                        put("trigger_source", context.triggerSource)
                        put("simulation", thermalUseCases.isSimulationMode())
                        putAll(context.metadata)
                    }

                try {
                    ensureDataManagementSession(context.sessionId, metadata)
                } catch (e: Exception) {
                    emitError("Failed to prepare session manifest: ${e.message}")
                }

                val recordingResult =
                    withContext(Dispatchers.IO) {
                        thermalUseCases.startRecording()
                    }
                return@withLock recordingResult
                    .onSuccess {
                        activeSession =
                            context.copy(sessionDirectory = sanitizedDir)
                        isRecording.set(true)
                        recordingStartElapsedMs = SystemClock.elapsedRealtime()
                        framesRecorded = 0
                        sessionMinTemp = Float.POSITIVE_INFINITY
                        sessionMaxTemp = Float.NEGATIVE_INFINITY
                        updateStatus {
                            copy(
                                isRecording = true,
                                recordingStartElapsedMs = recordingStartElapsedMs,
                                lastError = null,
                                isSimulation = thermalUseCases.isSimulationMode(),
                                frameDropCount = 0,
                            )
                        }
                        true
                    }.onFailure { throwable ->
                        emitError("Failed to start thermal recording: ${throwable.message ?: "unknown error"}")
                        false
                    }.getOrDefault(false)
            }

        suspend fun stopSession(): ThermalRecordingResult? =
            sessionMutex.withLock {
                if (!isRecording.get()) {
                    return@withLock null
                }

                val sessionContext = activeSession
                val stopResult =
                    withContext(Dispatchers.IO) {
                        thermalUseCases.stopRecording()
                    }

                val recordingDurationMs =
                    if (recordingStartElapsedMs > 0L) {
                        SystemClock.elapsedRealtime() - recordingStartElapsedMs
                    } else {
                        0L
                    }

                val finalResult =
                    stopResult.fold(
                        onSuccess = { path ->
                            path?.takeIf { it.isNotBlank() }?.let { moveAndRegisterRecording(it, sessionContext, recordingDurationMs) }
                        },
                        onFailure = { throwable ->
                            emitError("Failed to stop thermal recording: ${throwable.message ?: "unknown error"}")
                            null
                        },
                    )

                isRecording.set(false)
                activeSession = null
                recordingStartElapsedMs = 0L
                updateStatus {
                    copy(
                        isRecording = false,
                        recordingDurationMs = recordingDurationMs,
                        lastRecordingPath = finalResult?.file?.absolutePath ?: lastRecordingPath,
                        lastRecordingMetadata = finalResult?.metadata ?: lastRecordingMetadata,
                    )
                }
                lastRecordingResult = finalResult
                return@withLock finalResult
            }

        suspend fun startManualRecording(): Boolean {
            val sessionId = "thermal_manual_${System.currentTimeMillis()}"
            val sessionDir = File(appContext.filesDir, "thermal_manual/$sessionId")
            val context =
                ThermalSessionContext(
                    sessionId = sessionId,
                    sessionDirectory = sessionDir,
                    triggerSource = "LOCAL_UI",
                    metadata =
                        mapOf(
                            "session_type" to "manual",
                            "created_at" to System.currentTimeMillis(),
                        ),
                )
            return startSession(context)
        }

        suspend fun stopManualRecording(): ThermalRecordingResult? = stopSession()

        fun getLastRecordingResult(): ThermalRecordingResult? = lastRecordingResult

        suspend fun disconnect() =
            sessionMutex.withLock {
                if (isRecording.get()) {
                    stopSession()
                }
                streamingJobMutex.withLock {
                    streamingJob?.cancel()
                    streamingJob = null
                    isStreaming.set(false)
                }
                withContext(Dispatchers.IO) {
                    thermalUseCases.disconnectCamera()
                }
                isConnected.set(false)
                stopBatteryMonitoring()
                updateStatus {
                    copy(
                        isConnected = false,
                        isStreaming = false,
                        isRecording = false,
                        batteryPercent = null,
                        ispActive = false,
                    )
                }
            }

        fun shutdown() {
            streamingJob?.cancel()
            streamingJob = null
            isStreaming.set(false)
            stopBatteryMonitoring()
            scope.coroutineContext.cancel()
        }

        private fun startBatteryMonitoring() {
            if (batteryPollJob?.isActive == true) return
            batteryPollJob =
                scope.launch {
                    while (isConnected.get()) {
                        refreshBatteryStatus()
                        delay(BATTERY_POLL_INTERVAL_MS)
                    }
                }
        }

        private fun stopBatteryMonitoring() {
            batteryPollJob?.cancel()
            batteryPollJob = null
        }

        private suspend fun refreshBatteryStatus() {
            hardwareUseCases.getBatteryStatus
                .invoke()
                .onSuccess { status ->
                    updateStatus { copy(batteryPercent = status.level) }
                }
                .onFailure { throwable ->
                    logger.log(
                        StructuredLogger.LogLevel.DEBUG,
                        "ThermalCaptureCoordinator",
                        "battery_status_failed",
                        mapOf("error" to (throwable.message ?: "unknown")),
                    )
                }
        }

        private fun updateStatus(transform: ThermalStatus.() -> ThermalStatus) {
            _status.update { it.transform() }
        }

        private suspend fun startStreamingIfNeeded() =
            streamingJobMutex.withLock {
                if (isStreaming.get()) {
                    return@withLock
                }
                streamingJob =
                    scope.launch {
                        runCatching {
                            thermalUseCases
                                .startStreaming()
                                .collect { frame ->
                                    handleIncomingFrame(frame)
                                }
                        }.onFailure { throwable ->
                            emitError("Thermal streaming stopped: ${throwable.message ?: "unknown error"}")
                            isStreaming.set(false)
                            updateStatus { copy(isStreaming = false, ispActive = false) }
                        }
                    }
                isStreaming.set(true)
                updateStatus { copy(isStreaming = true, ispActive = true) }
            }

        private fun handleIncomingFrame(frame: ThermalFrameData) {
            val recordingActive = isRecording.get()
            if (recordingActive) {
                framesRecorded += 1
                sessionMinTemp = min(sessionMinTemp, frame.minTemp)
                sessionMaxTemp = max(sessionMaxTemp, frame.maxTemp)
            }
            val recordingDurationMs =
                if (recordingActive && recordingStartElapsedMs > 0L) {
                    SystemClock.elapsedRealtime() - recordingStartElapsedMs
                } else {
                    _status.value.recordingDurationMs
                }
            val expectedFrames =
                if (recordingActive) {
                    ((recordingDurationMs / 1000.0) * TARGET_FRAME_RATE_HZ).toLong()
                } else {
                    0L
                }
            val dropCount =
                if (recordingActive) {
                    max(0L, expectedFrames - framesRecorded)
                } else {
                    0L
                }
            val dropCountInt = minOf(dropCount, Int.MAX_VALUE.toLong()).toInt()
            _preview.value = frame
            updateStatus {
                copy(
                    isConnected = true,
                    isStreaming = true,
                    currentTemperature = frame.centerTemp,
                    minTemperature = frame.minTemp,
                    maxTemperature = frame.maxTemp,
                    frameCount = frameCount + 1,
                    lastFrameTimestampMs = frame.timestamp,
                    recordingDurationMs = recordingDurationMs,
                    ispActive = true,
                    frameDropCount = dropCountInt,
                )
            }
        }

        private fun ensureDataManagementSession(
            sessionId: String,
            metadata: Map<String, Any>,
        ) {
            val deviceId = buildDeviceId()
            val existing = dataManagementService.getSession(sessionId)
            if (existing == null) {
                dataManagementService.createSession(
                    sessionId = sessionId,
                    deviceId = deviceId,
                    customMetadata = metadata,
                )
            }
        }

        private fun moveAndRegisterRecording(
            originalPath: String,
            sessionContext: ThermalSessionContext?,
            recordingDurationMs: Long,
        ): ThermalRecordingResult? {
            val session = sessionContext ?: return null
            val sourceFile = File(originalPath)
            if (!sourceFile.exists()) {
                emitError("Thermal recording file missing: $originalPath")
                return null
            }

            val targetDir =
                session.sessionDirectory.apply {
                    if (!exists()) {
                        mkdirs()
                    }
                }
            val destinationFile = getAvailableDestination(targetDir, sourceFile.name)
            val manifestSource = File(sourceFile.parentFile, "${sourceFile.nameWithoutExtension}_manifest.json")
            val manifestDestination =
                if (manifestSource.exists()) {
                    getAvailableDestination(targetDir, manifestSource.name)
                } else {
                    null
                }

            runCatching {
                sourceFile.copyTo(destinationFile, overwrite = false)
                sourceFile.delete()
                manifestDestination?.let { dest ->
                    runCatching {
                        manifestSource.copyTo(dest, overwrite = false)
                        manifestSource.delete()
                    }
                }
            }.onFailure { throwable ->
                emitError("Unable to relocate thermal recording: ${throwable.message}")
            }

            val metadata =
                buildMap {
                    put("frames_recorded", framesRecorded)
                    put("duration_ms", recordingDurationMs)
                    put("min_temp", if (sessionMinTemp.isFinite()) sessionMinTemp else null)
                    put("max_temp", if (sessionMaxTemp.isFinite()) sessionMaxTemp else null)
                    put("simulation", thermalUseCases.isSimulationMode())
                    put("trigger_source", session.triggerSource)
                    putAll(session.metadata)
                }.filterValues { it != null }

            registerWithDataManagement(
                sessionId = session.sessionId,
                file = destinationFile,
                manifest = manifestDestination,
                metadata = metadata,
            )

            return ThermalRecordingResult(
                sessionId = session.sessionId,
                file = destinationFile,
                manifest = manifestDestination,
                metadata = metadata,
                framesRecorded = framesRecorded,
                minTemp = sessionMinTemp.takeIf { it.isFinite() },
                maxTemp = sessionMaxTemp.takeIf { it.isFinite() },
                simulation = thermalUseCases.isSimulationMode(),
            )
        }

        private fun registerWithDataManagement(
            sessionId: String,
            file: File,
            manifest: File?,
            metadata: Map<String, Any?>,
        ) {
            val deviceId = buildDeviceId()
            val sanitizedMetadata = metadata.filterValues { it != null }.mapValues { it.value!! }
            dataManagementService.registerFile(
                filePath = file.absolutePath,
                sessionId = sessionId,
                deviceId = deviceId,
                fileType = "thermal_data",
                customMetadata = sanitizedMetadata,
            )
            manifest?.let {
                dataManagementService.registerFile(
                    filePath = it.absolutePath,
                    sessionId = sessionId,
                    deviceId = deviceId,
                    fileType = "thermal_manifest",
                    customMetadata = mapOf("linked_file" to file.name),
                )
            }
        }

        private fun getAvailableDestination(directory: File, fileName: String): File {
            val base = File(directory, fileName)
            if (!base.exists()) {
                return base
            }
            val nameWithoutExt = base.nameWithoutExtension
            val ext = base.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
            var index = 1
            while (true) {
                val candidate = File(directory, "${nameWithoutExt}_$index$ext")
                if (!candidate.exists()) {
                    return candidate
                }
                index += 1
            }
        }

        private suspend fun emitError(message: String) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "ThermalCaptureCoordinator",
                "thermal_error",
                mapOf("message" to message),
            )
            _errors.emit(message)
            updateStatus { copy(lastError = message) }
        }

        private fun buildDeviceId(): String =
            "android_${android.os.Build.MODEL.replace(" ", "_")}"
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
