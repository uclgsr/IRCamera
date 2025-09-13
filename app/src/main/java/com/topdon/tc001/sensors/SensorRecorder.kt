package com.topdon.tc001.sensors

import kotlinx.coroutines.flow.Flow

/**
 * Core interface for all sensor recording implementations in the Multi-Modal Physiological Sensing Platform.
 *
 * This interface provides a consistent API for the RecordingController to manage
 * different sensor types (RGB camera, thermal camera, GSR) in a unified way.
 *
 * All sensor implementations must be lifecycle-aware and thread-safe.
 *
 * @author IRCamera Android Sensor Node (Spoke)
 */
interface SensorRecorder {
    /**
     * Unique identifier for this sensor recorder instance
     */
    val sensorId: String

    /**
     * Human-readable name for this sensor type
     */
    val sensorType: String

    /**
     * Current recording state of this sensor
     */
    val isRecording: Boolean

    /**
     * Data rate in samples/frames per second for this sensor
     */
    val samplingRate: Double

    /**
     * Initialize the sensor hardware and prepare for recording.
     * This should handle all hardware setup and validation.
     *
     * @return true if initialization successful, false otherwise
     */
    suspend fun initialize(): Boolean

    /**
     * Start recording sensor data.
     * Should be non-blocking and start background data capture.
     *
     * @param sessionDirectory Directory where sensor data should be stored
     * @return true if recording started successfully, false otherwise
     */
    suspend fun startRecording(sessionDirectory: String): Boolean

    /**
     * Stop recording sensor data.
     * Should cleanly stop all background operations and flush any buffered data.
     *
     * @return true if recording stopped successfully, false otherwise
     */
    suspend fun stopRecording(): Boolean

    /**
     * Add a synchronization marker to the data stream.
     * This is critical for temporal alignment across all sensors.
     *
     * @param markerType Type of sync marker (e.g., "flash", "manual", "auto")
     * @param timestampNs High-precision timestamp in nanoseconds
     * @param metadata Optional additional sync marker data
     */
    suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String> = emptyMap(),
    )

    /**
     * Clean up all resources and disconnect from hardware.
     * Should be called when the sensor is no longer needed.
     */
    suspend fun cleanup()

    /**
     * Flow of recording status updates.
     * Emits RecordingStatus updates for real-time monitoring.
     */
    fun getStatusFlow(): Flow<RecordingStatus>

    /**
     * Flow of error events from this sensor.
     * Critical for error handling and recovery in the RecordingController.
     */
    fun getErrorFlow(): Flow<SensorError>

    /**
     * Get current recording statistics.
     * Used for real-time monitoring and quality assurance.
     */
    fun getRecordingStats(): RecordingStats
}

/**
 * Recording status for real-time monitoring
 */
data class RecordingStatus(
    val sensorId: String,
    val sensorType: String,
    val isRecording: Boolean,
    val samplesRecorded: Long,
    val currentDataRate: Double,
    val storageUsedMB: Double,
    val timestampNs: Long,
)

/**
 * Error information from sensor operations
 */
data class SensorError(
    val sensorId: String,
    val sensorType: String,
    val errorType: ErrorType,
    val errorMessage: String,
    val timestampNs: Long,
    val isRecoverable: Boolean = true,
)

/**
 * Types of sensor errors for classification and handling
 */
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
    UNKNOWN,
}

/**
 * Recording statistics for monitoring and quality assurance
 */
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
