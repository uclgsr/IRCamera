package mpdc4gsr.gsr

import android.content.Context
import io.mockk.mockk
import java.io.File
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.recording.RecordingContext
import mpdc4gsr.gsr.session.TimelineClock

internal fun createRecordingContext(
    sessionDirectory: File,
    sessionId: String = "test-session",
    timelineClock: TimelineClock = mockk(relaxed = true),
    enabledModalities: Set<RecorderKind> = setOf(RecorderKind.GSR),
    simulationEnabled: Boolean = false,
): RecordingContext =
    RecordingContext(
        appContext = mockk<Context>(relaxed = true),
        sessionId = sessionId,
        sessionDirectory = sessionDirectory,
        clock = timelineClock,
        enabledModalities = enabledModalities,
        simulationEnabled = simulationEnabled,
    )
