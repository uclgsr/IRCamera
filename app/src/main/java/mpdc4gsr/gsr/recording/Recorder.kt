package mpdc4gsr.gsr.recording

import kotlinx.coroutines.flow.StateFlow
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState

interface Recorder : AutoCloseable {
    val kind: RecorderKind
    val state: StateFlow<RecorderState>

    suspend fun prepare(context: RecordingContext)
    suspend fun start(context: RecordingContext)
    suspend fun stop()
}
