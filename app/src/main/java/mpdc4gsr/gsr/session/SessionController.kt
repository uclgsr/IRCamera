package mpdc4gsr.gsr.session

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.model.DeviceDescriptor
import mpdc4gsr.gsr.model.FaultSeverity
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.model.SessionFault
import mpdc4gsr.gsr.model.SessionSnapshot
import mpdc4gsr.gsr.model.SessionStateStore
import mpdc4gsr.gsr.model.TimelineEstimate
import java.time.Instant
import java.util.UUID

/**
 * Coordinates session lifecycle updates for the Android capture node. It listens to command
 * broadcasts (from the PC), updates [SessionStateStore], and emits internal signals for the
 * capture coordinator and transfer services.
 */
class SessionController(
    private val context: Context,
    private val stateStore: SessionStateStore,
    private val clock: TimelineClock,
    private val dispatcher: CoroutineDispatcher,
) : AutoCloseable {

    private val mutex = Mutex()
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _commands = MutableSharedFlow<SessionCommand>(extraBufferCapacity = 32)
    val commands: SharedFlow<SessionCommand> = _commands.asSharedFlow()

    init {
        scope.launch {
            combine(
                clock.timeline,
                stateStore.sessionSnapshot.filterNotNull(),
            ) { timeline, snapshot ->
                snapshot.copy(globalTimeline = timeline)
            }.collect { updated ->
                stateStore.sessionSnapshot.value = updated
            }
        }
    }

    suspend fun applyCommand(command: SessionCommand) {
        mutex.withLock {
            when (command) {
                is SessionCommand.StartRecording -> onStartRecording(command)
                is SessionCommand.StopRecording -> onStopRecording(command)
                is SessionCommand.StimulusMarker -> onStimulusMarker(command)
                is SessionCommand.ApplyDeviceSnapshot -> onDeviceSnapshot(command.snapshot)
                is SessionCommand.UpdateRecorderState -> onRecorderState(command.kind, command.state)
                is SessionCommand.AppendFault -> onFault(command.fault)
                is SessionCommand.SetSimulationMode -> onSimulationMode(command.enabled)
            }
        }
        _commands.tryEmit(command)
    }

    suspend fun beginSession(
        sessionId: String,
        label: String,
        plannedDurationSeconds: Long?,
    ) {
        mutex.withLock {
            val now = clock.nowInstant()
            stateStore.sessionSnapshot.value =
                SessionSnapshot(
                    sessionId = sessionId,
                    label = label,
                    startedAt = now,
                    plannedDurationSeconds = plannedDurationSeconds,
                    elapsedMillis = 0,
                    isRecording = false,
                    connectedDevices = emptyList(),
                    recorderStates = RecorderKind.values().associateWith { RecorderState.IDLE },
                    faults = emptyList(),
                    previewStreams = emptyMap(),
                    globalTimeline = clock.timeline.value,
                    commandsPending = 0,
                )
        }
    }

    suspend fun endSession() {
        mutex.withLock {
            stateStore.sessionSnapshot.value = null
            stateStore.deviceTelemetry.value = emptyMap()
        }
    }

    private fun onStartRecording(command: SessionCommand.StartRecording) {
        stateStore.sessionSnapshot.value =
            stateStore.sessionSnapshot.value?.copy(
                isRecording = true,
                commandsPending = stateStore.sessionSnapshot.value?.commandsPending?.plus(1) ?: 1,
            )
    }

    private fun onStopRecording(command: SessionCommand.StopRecording) {
        stateStore.sessionSnapshot.value =
            stateStore.sessionSnapshot.value?.copy(
                isRecording = false,
                commandsPending = stateStore.sessionSnapshot.value?.commandsPending?.plus(1) ?: 1,
            )
    }

    private fun onStimulusMarker(command: SessionCommand.StimulusMarker) {
        val fault = SessionFault(
            id = UUID.randomUUID(),
            severity = FaultSeverity.INFO,
            source = null,
            message = "Stimulus marker '${command.markerId}' @ ${command.scheduledTimeMillis}ms",
            details = command.metadata,
            occurredAt = clock.nowInstant(),
        )
        onFault(fault)
    }

    private fun onDeviceSnapshot(snapshot: List<DeviceDescriptor>) {
        stateStore.sessionSnapshot.value =
            stateStore.sessionSnapshot.value?.copy(
                connectedDevices = snapshot,
            )
    }

    private fun onRecorderState(kind: RecorderKind, state: RecorderState) {
        stateStore.sessionSnapshot.value =
            stateStore.sessionSnapshot.value?.copy(
                recorderStates = stateStore.sessionSnapshot.value?.recorderStates
                    ?.toMutableMap()
                    ?.apply { this[kind] = state }
                    ?: mapOf(kind to state),
            )
    }

    private fun onFault(fault: SessionFault) {
        stateStore.sessionSnapshot.value =
            stateStore.sessionSnapshot.value?.copy(
                faults = stateStore.sessionSnapshot.value?.faults?.plus(fault) ?: listOf(fault),
            )
    }

    private fun onSimulationMode(enabled: Boolean) {
        val fault =
            SessionFault(
                severity = FaultSeverity.INFO,
                source = null,
                message = "Simulation mode ${if (enabled) "enabled" else "disabled"}",
                details = mapOf("enabled" to enabled),
            )
        onFault(fault)
    }

    fun updateTimelineEstimate(estimate: TimelineEstimate) {
        scope.launch { clock.updateEstimate(estimate) }
    }

    fun updateElapsed(elapsedMillis: Long) {
        stateStore.sessionSnapshot.value =
            stateStore.sessionSnapshot.value?.copy(elapsedMillis = elapsedMillis)
    }

    fun updateDeviceState(deviceId: String, state: ConnectionState) {
        val snapshot = stateStore.sessionSnapshot.value ?: return
        val updated = snapshot.connectedDevices.map { descriptor ->
            if (descriptor.id == deviceId) descriptor.copy(connectionState = state) else descriptor
        }
        stateStore.sessionSnapshot.value = snapshot.copy(connectedDevices = updated)
    }

    override fun close() {
        scope.cancel()
    }
}

sealed interface SessionCommand {
    data class StartRecording(
        val sessionId: String,
        val modalities: Set<RecorderKind>,
        val scheduledTimeMillis: Long,
        val parameters: Map<String, Any?> = emptyMap(),
    ) : SessionCommand

    data class StopRecording(
        val sessionId: String,
        val scheduledTimeMillis: Long,
    ) : SessionCommand

    data class StimulusMarker(
        val markerId: String,
        val scheduledTimeMillis: Long,
        val metadata: Map<String, Any?> = emptyMap(),
    ) : SessionCommand

    data class ApplyDeviceSnapshot(val snapshot: List<DeviceDescriptor>) : SessionCommand
    data class UpdateRecorderState(val kind: RecorderKind, val state: RecorderState) : SessionCommand
    data class AppendFault(val fault: SessionFault) : SessionCommand
}
