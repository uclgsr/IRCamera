package mpdc4gsr.feature.network.data

// Recording state enums
enum class RecordingState {
    IDLE,
    STOPPED,
    STARTING,
    RECORDING,
    STOPPING,
    ERROR
}
enum class TriggerSource {
    LOCAL_UI,
    LOCAL_NOTIFICATION,
    REMOTE_PC,
    AUTOMATIC,
    CRASH_RECOVERY
}
enum class SessionState {
    IDLE,
    STARTING,
    RECORDING,
    ACTIVE,
    STOPPING,
    COMPLETED,
    STOPPED_COMPLETED,
    STOPPED_FAILED,
    STOPPED_INCOMPLETE,
    FAILED,
    CANCELLED
}
// Session orchestration data classes
data class SessionManifest(
    val sessionId: String,
    val startTime: Long,
    val stopTime: Long? = null,
    val duration: Long? = null,
    val triggerSource: TriggerSource = TriggerSource.LOCAL_UI,
    val sensorActivitySummary: Map<String, SensorActivityInfo> = emptyMap(),
    val events: List<SessionEvent> = emptyList(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val fileReferences: Map<String, String> = emptyMap(),
    val sessionState: SessionState = SessionState.COMPLETED
)
data class SessionEvent(
    val eventType: String,
    val timestampMs: Long,
    val sensorId: String? = null,
    val triggerSource: TriggerSource? = null,
    val metadata: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val errorMessage: String? = null
)
data class SensorActivityInfo(
    val sensorName: String,
    val wasActive: Boolean,
    val startedSuccessfully: Boolean = true,
    val finalStatus: String = if (wasActive) "ACTIVE" else "INACTIVE",
    val errorMessages: List<String> = emptyList(),
    val dropouts: List<DropoutEvent> = emptyList(),
    val reconnections: List<ReconnectionEvent> = emptyList()
)
data class SensorHealthInfo(
    val sensorId: String,
    val isHealthy: Boolean,
    val lastHealthCheck: Long,
    val healthScore: Double = 1.0,
    val issues: List<String> = emptyList(),
    val consecutiveFailures: Int = 0,
    val lastError: String? = null
)
data class DropoutEvent(
    val sensorId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val timestampMs: Long = startTime,
    val reason: String? = null,
    val durationMs: Long = if (endTime != null) endTime - startTime else 0L,
    val recoverable: Boolean = true
)
data class ReconnectionEvent(
    val sensorId: String,
    val timestamp: Long,
    val timestampMs: Long = timestamp,
    val successful: Boolean,
    val attemptCount: Int = 1,
    val attemptNumber: Int = attemptCount,
    val delayMs: Long = 0L,
    val errorMessage: String? = null
)
// Recording status and statistics
data class RecordingStats(
    val sessionId: String,
    val duration: Long,
    val activeSensors: Int,
    val totalSamples: Long,
    val avgDataRate: Double,
    val storageUsedMB: Double,
    val errors: Int,
    val warnings: Int,
    val qualityScore: Double = 1.0
)
data class SensorStatusInfo(
    val sensorId: String,
    val isActive: Boolean,
    val isHealthy: Boolean,
    val lastSampleTime: Long,
    val samplesRecorded: Long,
    val errorCount: Int
)
// Legacy compatibility for ComprehensiveRecordingController
data class SensorHealthSummary(
    val sensorId: String,
    val name: String,
    val isHealthy: Boolean
)
data class SessionInfoData(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val durationSeconds: Double,
    val recordingStatus: String,
    val activeSensors: List<String>,
    val sensorStopResults: Map<String, Boolean>,
    val errors: List<String>?,
    val finalizedAt: Long
)
data class ValidationResult(
    val isValid: Boolean,
    val failureReason: String = ""
)
data class RecordingError(
    val timestamp: Long,
    val sensorId: String?,
    val errorType: String,
    val message: String,
    val isRecoverable: Boolean = true
)
// Hardware validation types
data class ValidationReport(
    val timestamp: Long,
    val deviceInfo: DeviceInfo,
    val validationResults: Map<String, HardwareValidationResult>,
    val sensorCapabilities: Map<String, SensorCapability>,
    val performanceMetrics: Map<String, Any>,
    val errorLogs: List<String>,
    val summary: ValidationSummary
)
data class HardwareValidationResult(
    val sensorId: String,
    val isOperational: Boolean,
    val capabilities: List<SensorCapability>,
    val issues: List<String>
)
data class SensorCapability(
    val name: String,
    val isSupported: Boolean,
    val details: String
)
data class ValidationSummary(
    val totalSensors: Int,
    val operationalSensors: Int,
    val criticalIssuesCount: Int,
    val overallHealthScore: Double,
    val readyForRecording: Boolean
)
data class DeviceInfo(
    val deviceId: String,
    val model: String,
    val androidVersion: String,
    val availableStorageGB: Double,
    val batteryLevel: Int
)
// Simple recording status for basic status reporting
data class SimpleRecordingStatus(
    val isRecording: Boolean,
    val activeSensors: Int,
    val totalSensors: Int,
    val state: RecordingState
)
typealias MainRecordingState = RecordingState