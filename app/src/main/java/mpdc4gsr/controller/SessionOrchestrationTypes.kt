package mpdc4gsr.controller

// Session orchestration types for comprehensive session lifecycle management
data class SessionManifest(
    val sessionId: String,
    val startTime: Long,
    val stopTime: Long?,
    val duration: Long?,
    val triggerSource: RecordingController.TriggerSource,
    val sensorActivitySummary: Map<String, SensorActivityInfo>,
    val events: List<SessionEvent>,
    val errors: List<String>,
    val warnings: List<String>,
    val fileReferences: Map<String, String>,
    val sessionState: RecordingController.SessionState
)

data class SensorActivityInfo(
    val sensorName: String,
    val wasActive: Boolean,
    val startedSuccessfully: Boolean,
    val finalStatus: String,
    val errorMessages: List<String>,
    val dropouts: List<DropoutEvent> = emptyList(),
    val reconnections: List<ReconnectionEvent> = emptyList()
)

data class SessionEvent(
    val eventType: String,
    val timestampMs: Long,
    val sensorId: String? = null,
    val triggerSource: RecordingController.TriggerSource? = null,
    val metadata: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val errorMessage: String? = null
)

data class SensorHealthInfo(
    val sensorId: String,
    val isHealthy: Boolean,
    val lastHealthCheck: Long,
    val consecutiveFailures: Int,
    val lastError: String? = null
)

data class DropoutEvent(
    val timestampMs: Long,
    val reason: String,
    val durationMs: Long
)

data class ReconnectionEvent(
    val timestampMs: Long,
    val attemptNumber: Int,
    val successful: Boolean,
    val delayMs: Long
)
