package mpdc4gsr.gsr.capture

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.recording.Recorder
import mpdc4gsr.gsr.recording.RecordingContext
import mpdc4gsr.gsr.session.SessionCommand
import mpdc4gsr.gsr.session.SessionController

/**
 * Orchestrates modality recorders, ensuring synchronous start/stop aligned with the PC session
 * clock (FR2). The coordinator also updates recorder states via [SessionController].
 */
class CaptureCoordinator(
    private val recorderFactory: RecorderFactory,
    private val sessionController: SessionController,
    private val dispatcher: CoroutineDispatcher,
) : AutoCloseable {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val recorders = mutableMapOf<RecorderKind, Recorder>()

    suspend fun prepare(context: RecordingContext) {
        withContext(dispatcher) {
            context.enabledModalities.forEach { modality ->
                val recorder = recorderFactory.create(modality)
                recorders[modality] = recorder
                sessionController.applyCommand(
                    SessionCommand.UpdateRecorderState(modality, RecorderState.PREPARING),
                )
                recorder.prepare(context)
                sessionController.applyCommand(
                    SessionCommand.UpdateRecorderState(modality, RecorderState.IDLE),
                )
            }
        }
    }

    suspend fun startRecording(context: RecordingContext) {
        withContext(dispatcher) {
            recorders.forEach { (kind, recorder) ->
                sessionController.applyCommand(SessionCommand.UpdateRecorderState(kind, RecorderState.RECORDING))
                recorder.start(context)
            }
        }
    }

    suspend fun stopRecording() {
        withContext(dispatcher) {
            recorders.forEach { (kind, recorder) ->
                sessionController.applyCommand(SessionCommand.UpdateRecorderState(kind, RecorderState.STOPPING))
                recorder.stop()
                sessionController.applyCommand(SessionCommand.UpdateRecorderState(kind, RecorderState.IDLE))
            }
        }
    }

    override fun close() {
        scope.cancel()
        recorders.values.forEach { runCatching { it.close() } }
        recorders.clear()
    }
}
