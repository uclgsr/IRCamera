package mpdc4gsr.controller

/**
 * Represents the session manifest containing session metadata and configuration
 */
data class SessionManifest(
    val sessionId: String,
    val startTime: Long,
    val stopTime: Long? = null,
    val duration: Long? = null,
    val triggerSource: String? = null,
    val sensorActivitySummary: Map<String, SensorActivityInfo> = emptyMap(),
    val events: List<SessionEvent> = emptyList(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val fileReferences: Map<String, Any> = emptyMap(),
    val sessionState: String = "COMPLETED"
)

/**
 * Represents a session event for tracking recording activities
 */
data class SessionEvent(
    val eventType: String,
    val timestampMs: Long,
    val sensorId: String? = null,
    val triggerSource: String,
    val metadata: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Sensor activity information for comprehensive recording
 */
data class SensorActivityInfo(
    val sensorName: String,
    val wasActive: Boolean,
    val startedSuccessfully: Boolean = true,
    val finalStatus: String = if (wasActive) "ACTIVE" else "INACTIVE",
    val errorMessages: List<String> = emptyList(),
    val dropouts: List<DropoutEvent> = emptyList(),
    val reconnections: List<ReconnectionEvent> = emptyList()
)

/**
 * Sensor health information for monitoring
 */
data class SensorHealthInfo(
    val sensorId: String,
    val isHealthy: Boolean,
    val lastHealthCheck: Long,
    val healthScore: Double = 1.0,
    val issues: List<String> = emptyList(),
    val consecutiveFailures: Int = 0,
    val lastError: String? = null
)

/**
 * Comprehensive sensor health information (alias for compatibility)
 */
typealias ComprehensiveSensorHealthInfo = SensorHealthInfo

/**
 * Dropout event tracking for resilient recording
 */
data class DropoutEvent(
    val sensorId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val reason: String? = null,
    val recoverable: Boolean = true
)

/**
 * Reconnection event tracking
 */
data class ReconnectionEvent(
    val sensorId: String,
    val timestamp: Long,
    val successful: Boolean,
    val attemptCount: Int = 1,
    val errorMessage: String? = null
)