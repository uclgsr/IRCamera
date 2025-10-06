package mpdc4gsr.core.data
import kotlinx.coroutines.flow.Flow
interface SensorRecorder {
    val sensorId: String
    val sensorType: String
    val isRecording: Boolean
    val samplingRate: Double
    suspend fun initialize(): Boolean
    suspend fun startRecording(sessionDirectory: String): Boolean
    suspend fun startRecording(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata
    ): Boolean {
        // Default implementation delegates to original method for backward compatibility
        return startRecording(sessionDirectory)
    }
    suspend fun stopRecording(): Boolean
    suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String> = emptyMap(),
    )
    suspend fun cleanup()
    fun getStatusFlow(): Flow<RecordingStatus>
    fun getErrorFlow(): Flow<SensorError>
    fun getRecordingStats(): RecordingStats
}
data class RecordingStatus(
    val sensorId: String,
    val sensorType: String,
    val isRecording: Boolean,
    val samplesRecorded: Long,
    val currentDataRate: Double,
    val storageUsedMB: Double,
    val timestampNs: Long,
) {
    val displayText: String
        get() = if (isRecording) {
            "Recording: $samplesRecorded samples @ ${String.format("%.1f", currentDataRate)} Hz"
        } else {
            "Ready - ${String.format("%.1f", storageUsedMB)} MB"
        }
}
data class SensorError(
    val sensorId: String,
    val sensorType: String,
    val errorType: ErrorType,
    val errorMessage: String,
    val timestampNs: Long,
    val isRecoverable: Boolean = true,
)
enum class ErrorType {
    INITIALIZATION_FAILED,
    HARDWARE_DISCONNECTED,
    RECORDING_FAILED,
    STORAGE_FULL,
    PERMISSION_DENIED,
    SYNC_FAILED,
    DATA_CORRUPTION,
    DEVICE_ERROR,
    STORAGE_ERROR,
    CONNECTION_LOST,
    CONNECTION_RESTORED,  // Added for enhanced reconnection feedback
    PAIRING_REQUIRED,
    DATA_PROCESSING_ERROR,
    FEATURE_NOT_SUPPORTED,
    HARDWARE_UNAVAILABLE,
    OPERATION_FAILED,
    DEVICE_NOT_SUPPORTED,
    UNKNOWN,
}
data class RecordingStats(
    val sensorId: String,
    val sensorType: String,
    val sessionDurationMs: Long,
    val totalSamplesRecorded: Long,
    val averageDataRate: Double,
    val droppedSamples: Long,
    val storageUsedMB: Double,
    val syncMarkersCount: Int,
    val lastSampleTimestampNs: Long,
)
