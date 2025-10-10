package mpdc4gsr.gsr

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import mpdc4gsr.feature.capture.thermal.data.ThermalCaptureCoordinator
import mpdc4gsr.feature.capture.thermal.data.ThermalStatus
import mpdc4gsr.gsr.capture.CaptureCoordinator
import mpdc4gsr.gsr.capture.RecorderFactory
import mpdc4gsr.gsr.capture.SimulationSource
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.model.DeviceDescriptor
import mpdc4gsr.gsr.model.DeviceType
import mpdc4gsr.gsr.model.FaultSeverity
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.model.SessionFault
import mpdc4gsr.gsr.model.SessionStateStore
import mpdc4gsr.gsr.model.TelemetryState
import mpdc4gsr.gsr.network.CommandClient
import mpdc4gsr.gsr.network.PreviewPublisher
import mpdc4gsr.gsr.network.TimeSyncClient
import mpdc4gsr.gsr.network.TransferClient
import mpdc4gsr.gsr.recording.RecordingContext
import mpdc4gsr.gsr.session.SessionCommand
import mpdc4gsr.gsr.session.SessionController
import mpdc4gsr.gsr.session.TimelineClock

/**
 * High-level facade for the Android GSR subsystem. Wires together networking, device management,
 * capture coordination, and post-session transfers.
 */
class GsrOrchestrator(
    private val context: Context,
    private val lifecycleOwnerProvider: () -> LifecycleOwner,
    private val ioDispatcher: CoroutineDispatcher,
    private val sessionDirectoryProvider: () -> File,
    private val shimmerController: ShimmerDeviceController,
    private val commandClient: CommandClient,
    private val timeSyncClient: TimeSyncClient,
    private val transferClient: TransferClient,
    private val recorderFactory: RecorderFactory,
    private val sessionController: SessionController,
    private val captureCoordinator: CaptureCoordinator,
    private val timelineClock: TimelineClock,
    private val sessionStateStore: SessionStateStore,
    private val previewPublisher: PreviewPublisher,
    private val thermalCoordinator: ThermalCaptureCoordinator,
    private val simulationSource: SimulationSource,
) : AutoCloseable {

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var activeRecordingContext: RecordingContext? = null
    @Volatile private var started = false
    private val deviceSnapshotMutex = Mutex()
    private val deviceSnapshot = mutableMapOf<String, DeviceDescriptor>()
    private var lastThermalStatus: ThermalStatus? = null
    private var lastThermalFrameTimestampMs: Long? = null
    private var lastThermalFrameCount: Long = 0

    private companion object {
        private const val THERMAL_DEVICE_ID = "thermal_camera_local"
    }

    fun start() {
        if (started) return
        started = true
        commandClient.start()
        timeSyncClient.start()
        previewPublisher.start()
        scope.launch {
            runCatching { thermalCoordinator.ensureConnected() }
        }
        scope.launch {
            commandClient.events.collectLatest { command ->
                when (command) {
                    is SessionCommand.StartRecording -> handleStartRecording(command)
                    is SessionCommand.StopRecording -> handleStopRecording(command)
                    is SessionCommand.StimulusMarker -> sessionController.applyCommand(command)
                    is SessionCommand.ApplyDeviceSnapshot -> sessionController.applyCommand(command)
                    is SessionCommand.UpdateRecorderState -> sessionController.applyCommand(command)
                    is SessionCommand.AppendFault -> sessionController.applyCommand(command)
                    is SessionCommand.SetSimulationMode -> {
                        sessionController.applyCommand(command)
                        handleSimulation(command.enabled)
                    }
                }
            }
        }
        scope.launch {
            shimmerController.devices.collectLatest { devices ->
                deviceSnapshotMutex.withLock {
                    deviceSnapshot.entries.removeIf { it.value.type == DeviceType.SHIMMER_GSR_SENSOR }
                    deviceSnapshot.putAll(devices)
                    sessionController.applyCommand(
                        SessionCommand.ApplyDeviceSnapshot(deviceSnapshot.values.toList()),
                    )
                }
            }
        }
        scope.launch {
            shimmerController.telemetry.collectLatest { telemetry ->
                sessionStateStore.deviceTelemetry.value = telemetry.toMap()
            }
        }
        scope.launch {
            thermalCoordinator.statusFlow().collectLatest { status ->
                handleThermalStatus(status)
            }
        }
    }

    private suspend fun handleStartRecording(command: SessionCommand.StartRecording) {
        val sessionFolder = File(sessionDirectoryProvider(), command.sessionId).apply { mkdirs() }
        val recordingContext =
            RecordingContext(
                appContext = context.applicationContext,
                sessionId = command.sessionId,
                sessionDirectory = sessionFolder,
                clock = timelineClock,
                enabledModalities = command.modalities,
                simulationEnabled = command.parameters["simulation"] as? Boolean ?: false,
            )
        sessionController.beginSession(
            sessionId = command.sessionId,
            label = command.parameters["label"]?.toString() ?: "Session-${command.sessionId}",
            plannedDurationSeconds = (command.parameters["plannedDurationSeconds"] as? Number)?.toLong(),
        )
        captureCoordinator.prepare(recordingContext)
        activeRecordingContext = recordingContext
        captureCoordinator.startRecording(recordingContext)
    }

    private suspend fun handleStopRecording(command: SessionCommand.StopRecording) {
        captureCoordinator.stopRecording()
        val context = activeRecordingContext ?: return
        withContext(ioDispatcher) {
            uploadOutputs(context)
        }
        activeRecordingContext = null
        sessionController.endSession()
    }

    private suspend fun handleThermalStatus(status: ThermalStatus) {
        updateThermalTelemetry(status)
        updateThermalDeviceDescriptor(status)
        updateThermalRecorderState(status)
        emitThermalFaultIfNeeded(status)
        lastThermalStatus = status
    }

    private fun updateThermalTelemetry(status: ThermalStatus) {
        val current = sessionStateStore.deviceTelemetry.value.toMutableMap()
        val base =
            current[THERMAL_DEVICE_ID]
                ?: TelemetryState(
                    gsrMicrosiemens = null,
                    skinTemperatureCelsius = null,
                    thermalSpotCelsius = null,
                    audioLevelDb = null,
                    frameRate = null,
                    droppedFrames = 0,
                    batteryPercent = null,
                    rssi = null,
                )
        val frameRate = computeThermalFrameRate(status) ?: base.frameRate
        current[THERMAL_DEVICE_ID] =
            base.copy(
                thermalSpotCelsius = status.currentTemperature?.toFloat() ?: base.thermalSpotCelsius,
                frameRate = frameRate,
            )
        sessionStateStore.deviceTelemetry.value = current
    }

    private suspend fun updateThermalDeviceDescriptor(status: ThermalStatus) {
        deviceSnapshotMutex.withLock {
            val changed =
                if (!status.isConnected) {
                    deviceSnapshot.remove(THERMAL_DEVICE_ID) != null
                } else {
                    val descriptor = buildThermalDescriptor(status)
                    val previous = deviceSnapshot[THERMAL_DEVICE_ID]
                    deviceSnapshot[THERMAL_DEVICE_ID] = descriptor
                    previous != descriptor
                }
            if (changed) {
                sessionController.applyCommand(
                    SessionCommand.ApplyDeviceSnapshot(deviceSnapshot.values.toList()),
                )
            }
        }
    }

    private suspend fun updateThermalRecorderState(status: ThermalStatus) {
        val previous = lastThermalStatus
        if (previous?.isRecording != status.isRecording) {
            val newState =
                if (status.isRecording) {
                    RecorderState.RECORDING
                } else {
                    RecorderState.IDLE
                }
            sessionController.applyCommand(
                SessionCommand.UpdateRecorderState(RecorderKind.THERMAL_VIDEO, newState),
            )
        } else if (
            previous?.isStreaming != status.isStreaming &&
            !status.isRecording
        ) {
            val newState =
                if (status.isStreaming) {
                    RecorderState.PREPARING
                } else {
                    RecorderState.IDLE
                }
            sessionController.applyCommand(
                SessionCommand.UpdateRecorderState(RecorderKind.THERMAL_VIDEO, newState),
            )
        }
    }

    private suspend fun emitThermalFaultIfNeeded(status: ThermalStatus) {
        val error = status.lastError
        val previousError = lastThermalStatus?.lastError
        if (error != null && error != previousError) {
            val fault =
                SessionFault(
                    severity = FaultSeverity.ERROR,
                    source = RecorderKind.THERMAL_VIDEO,
                    message = error,
                    details =
                        mapOf(
                            "simulation" to status.isSimulation.toString(),
                            "is_connected" to status.isConnected.toString(),
                        ),
                    occurredAt = timelineClock.nowInstant(),
                )
            sessionController.applyCommand(SessionCommand.AppendFault(fault))
        }
    }

    private fun computeThermalFrameRate(status: ThermalStatus): Double? {
        val timestampMs = status.lastFrameTimestampMs ?: return null
        val previousTimestamp = lastThermalFrameTimestampMs
        val previousCount = lastThermalFrameCount
        lastThermalFrameTimestampMs = timestampMs
        lastThermalFrameCount = status.frameCount
        if (previousTimestamp == null || status.frameCount <= previousCount) {
            return null
        }
        val deltaMs = (timestampMs - previousTimestamp).coerceAtLeast(1)
        val deltaFrames = status.frameCount - previousCount
        return deltaFrames * 1000.0 / deltaMs
    }

    private fun buildThermalDescriptor(status: ThermalStatus): DeviceDescriptor {
        val connectionState =
            when {
                status.isRecording -> ConnectionState.RECORDING
                status.isStreaming -> ConnectionState.READY
                status.isConnected -> ConnectionState.CONNECTED
                else -> ConnectionState.DISCONNECTED
            }
        return DeviceDescriptor(
            id = THERMAL_DEVICE_ID,
            displayName = "Thermal Camera",
            type = DeviceType.ANDROID_HOST,
            connectionState = connectionState,
            batteryPercent = null,
            supportsThermal = true,
            supportsRgb = false,
            supportsAudio = false,
            shimmerMacAddress = null,
            lastHeartbeat = Instant.now(),
            timeOffsetMillis = null,
            tags = setOf("thermal", "local"),
        )
    }

    fun launchSimulationSession(modalities: Set<RecorderKind> = RecorderKind.values().toSet()) {
        scope.launch {
            val sessionId = "sim-${UUID.randomUUID()}"
            val command =
                SessionCommand.StartRecording(
                    sessionId = sessionId,
                    modalities = modalities,
                    scheduledTimeMillis = timelineClock.nowInstant().toEpochMilli(),
                    parameters =
                        mapOf(
                            "label" to "Simulation Session",
                            "simulation" to true,
                            "plannedDurationSeconds" to 300,
                        ),
                )
            handleStartRecording(command)
        }
    }

    fun stopSession() {
        scope.launch {
            val sessionId = activeRecordingContext?.sessionId ?: return@launch
            val command =
                SessionCommand.StopRecording(
                    sessionId = sessionId,
                    scheduledTimeMillis = timelineClock.nowInstant().toEpochMilli(),
                )
            handleStopRecording(command)
        }
    }

    private suspend fun uploadOutputs(context: RecordingContext) {
        val deviceId = android.os.Build.DEVICE
        context.sessionDirectory.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val modality = RecorderKind.values().firstOrNull { file.name.contains(it.name, true) }?.name ?: "unknown"
                transferClient.uploadFile(context.sessionId, deviceId, file, modality)
            }
    }

    private fun handleSimulation(enabled: Boolean) {
        if (enabled) {
            val sessionId = activeRecordingContext?.sessionId ?: "preview"
            shimmerController.enableSimulation(sessionId)
        } else {
            shimmerController.disableSimulation()
        }
    }

    override fun close() {
        started = false
        scope.cancel()
        commandClient.stop()
        captureCoordinator.close()
        previewPublisher.close()
        shimmerController.close()
        timeSyncClient.close()
        shimmerController.disableSimulation()
        simulationSource.stop()
        sessionStateStore.sessionSnapshot.value = null
        sessionStateStore.deviceTelemetry.value = emptyMap()
    }
}

