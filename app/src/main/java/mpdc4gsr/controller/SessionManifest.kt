package mpdc4gsr.controller

/**
 * Represents the session manifest containing session metadata and configuration
 */
data class SessionManifest(
    val sessionId: String,
    val sessionDirectory: String,
    val startTime: Long,
    val endTime: Long? = null,
    val sensors: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap(),
    val events: List<SessionEvent> = emptyList(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val duration: Long = (endTime?.minus(startTime)) ?: 0L,
    val triggerSource: String? = null,
    val sensorActivitySummary: Map<String, Any> = emptyMap(),
    val fileReferences: List<String> = emptyList(),
    val sessionState: String = "COMPLETED"
)

/**
 * Represents a session event for tracking recording activities
 */
data class SessionEvent(
    val eventType: String,
    val timestamp: Long,
    val sensorId: String? = null,
    val success: Boolean = true,
    val errorMessage: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val triggerSource: String? = null
)

/**
 * Sensor activity information for comprehensive recording
 */
data class SensorActivityInfo(
    val sensorId: String,
    val isActive: Boolean,
    val lastActivityTime: Long,
    val totalSamples: Long = 0,
    val errorCount: Long = 0,
    val startedSuccessfully: Boolean = true,
    val finalStatus: String = if (isActive) "ACTIVE" else "INACTIVE",
    val errorMessages: List<String> = emptyList()
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