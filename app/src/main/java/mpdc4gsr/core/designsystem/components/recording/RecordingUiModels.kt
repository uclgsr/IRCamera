package mpdc4gsr.core.designsystem.components.recording

/**
 * High level recording UI states so we can share them between different dashboards.
 */
enum class RecordingUiState {
    Idle,
    Starting,
    Recording,
    Stopping,
    Error,
}

enum class RecordingTriggerSource {
    Local,
    RemotePc,
    RemoteMobile,
    Scheduled,
}

data class RecordingSessionSummary(
    val state: RecordingUiState,
    val triggerSource: RecordingTriggerSource,
    val duration: String = "00:00:00",
    val sessionId: String = "",
    val dataSize: String = "0 MB",
    val frameCount: Int = 0,
    val errorMessage: String? = null,
    val startTimeMillis: Long = 0L,
)
