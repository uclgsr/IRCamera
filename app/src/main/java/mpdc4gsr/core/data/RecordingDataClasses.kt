package mpdc4gsr.core.data

data class SessionManifest(
    val sessionId: String,
    val startTimestamp: Long,
    val activeSensors: List<String>,
    val sessionDirectory: String
)

data class SessionEvent(
    val eventType: String,
    val timestampMs: Long,
    val sensorId: String? = null,
    val triggerSource: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val errorMessage: String? = null
)

data class SensorActivityInfo(
    val sensorName: String,
    val wasActive: Boolean,
    val startedSuccessfully: Boolean,
    val finalStatus: String,
    val errorMessages: List<String> = emptyList(),
    val samplesCollected: Long = 0,
    val lastActivityTimestamp: Long = System.currentTimeMillis()
)

data class SensorHealthInfo(
    val sensorId: String,
    val isHealthy: Boolean,
    val lastHealthCheck: Long,
    val consecutiveFailures: Int = 0,
    val lastError: String? = null
)

data class DropoutEvent(
    val sensorType: String,
    val timestamp: Long,
    val reason: String
)

data class ReconnectionEvent(
    val sensorType: String,
    val timestamp: Long,
    val successful: Boolean,
    val attemptNumber: Int
)