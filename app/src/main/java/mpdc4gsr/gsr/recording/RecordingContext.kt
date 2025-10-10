package mpdc4gsr.gsr.recording

import android.content.Context
import java.io.File
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.session.TimelineClock

data class RecordingContext(
    val appContext: Context,
    val sessionId: String,
    val sessionDirectory: File,
    val clock: TimelineClock,
    val enabledModalities: Set<RecorderKind>,
    val simulationEnabled: Boolean,
)
