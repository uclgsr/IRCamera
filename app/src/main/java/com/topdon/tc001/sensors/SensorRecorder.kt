package com.topdon.tc001.sensors

import kotlinx.coroutines.flow.Flow


interface SensorRecorder {

    val sensorId: String


    val sensorType: String


    val isRecording: Boolean


    val samplingRate: Double


    suspend fun initialize(): Boolean


    suspend fun startRecording(sessionDirectory: String): Boolean


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
)


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
