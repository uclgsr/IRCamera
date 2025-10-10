package mpdc4gsr.gsr.recording

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import mpdc4gsr.feature.capture.thermal.data.ThermalCaptureCoordinator
import mpdc4gsr.feature.capture.thermal.data.ThermalSessionContext
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState

/**
 * Adapts the shared [ThermalCaptureCoordinator] to the legacy recorder abstraction used by the
 * GSR orchestration layer. This ensures all thermal recording flows (PC orchestration, manual UI,
 * crash recovery) share a single implementation.
 */
class CoordinatorThermalRecorder(
    private val coordinator: ThermalCaptureCoordinator,
) : Recorder {

    override val kind: RecorderKind = RecorderKind.THERMAL_VIDEO

    private val _state = MutableStateFlow(RecorderState.IDLE)
    override val state: StateFlow<RecorderState> = _state

    private var sessionContext: ThermalSessionContext? = null

    override suspend fun prepare(context: RecordingContext) {
        _state.value = RecorderState.PREPARING
        val connected = withContext(Dispatchers.IO) { coordinator.ensureConnected() }
        _state.value = if (connected) RecorderState.IDLE else RecorderState.FAILED
    }

    override suspend fun start(context: RecordingContext) {
        _state.value = RecorderState.RECORDING
        val thermalDir = File(context.sessionDirectory, "Thermal")
        val thermalSession =
            ThermalSessionContext(
                sessionId = context.sessionId,
                sessionDirectory = thermalDir,
                triggerSource = "GSR_ORCHESTRATOR",
                metadata =
                    mapOf(
                        "simulation" to context.simulationEnabled,
                        "origin" to "gsr_capture_coordinator",
                    ),
            )
        val success =
            withContext(Dispatchers.IO) {
                coordinator.startSession(thermalSession)
            }
        if (!success) {
            _state.value = RecorderState.FAILED
        } else {
            sessionContext = thermalSession
        }
    }

    override suspend fun stop() {
        _state.value = RecorderState.STOPPING
        withContext(Dispatchers.IO) {
            coordinator.stopSession()
        }
        _state.value = RecorderState.IDLE
        sessionContext = null
    }

    override fun close() {
        sessionContext = null
    }
}
