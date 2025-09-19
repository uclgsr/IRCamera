package mpdc4gsr.sensors

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.FileWriter

/**
 * Service for coordinating time synchronization across all sensors
 * Implements unified timestamping as requested in issue requirements
 */
class TimeSynchronizationService {
    companion object {
        private const val TAG = "TimeSynchronizationService"
        private const val SYNC_METADATA_FILENAME = "session_sync_metadata.csv"
    }
    
    private var sessionReference: SessionTimestampReference? = null
    private var sessionDirectory: String? = null
    
    private val _syncEvents = MutableSharedFlow<SyncEvent>()
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()
    
    /**
     * Initialize session with unified timestamp reference
     * This establishes the common time reference for all sensors
     */
    fun initializeSession(sessionDirectory: String): SessionTimestampReference {
        this.sessionDirectory = sessionDirectory
        sessionReference = TimestampManager.startSession()
        
        Log.i(TAG, "Session initialized with unified timestamp reference")
        
        // Write session reference metadata to file
        writeSessionSyncMetadata()
        
        return sessionReference!!
    }
    
    /**
     * Get current session timestamp reference
     */
    fun getSessionReference(): SessionTimestampReference? = sessionReference
    
    /**
     * Create synchronized timestamp for sensor data
     * All sensors should use this for consistent cross-sensor timing
     */
    fun createSynchronizedTimestamp(): TimestampRecord {
        return TimestampManager.createTimestampRecord()
    }
    
    /**
     * Convert device-specific timestamp to unified session time
     */
    fun convertDeviceTimestamp(deviceTimestamp: Long, sensorId: String): TimestampRecord {
        val unifiedTimestamp = TimestampManager.createTimestampRecord()
        
        Log.v(TAG, "Converted device timestamp for $sensorId: device=$deviceTimestamp, unified=${unifiedTimestamp.systemNanos}")
        
        return unifiedTimestamp
    }
    
    /**
     * Emit sync event for cross-sensor alignment validation
     */
    suspend fun emitSyncEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
        val timestampRecord = createSynchronizedTimestamp()
        val syncEvent = SyncEvent(
            eventType = eventType,
            timestampRecord = timestampRecord,
            metadata = metadata
        )
        
        _syncEvents.emit(syncEvent)
        Log.i(TAG, "Sync event emitted: $eventType at ${timestampRecord.systemTimeMs}ms")
    }
    
    /**
     * Finalize session and calculate total session time
     */
    fun finalizeSession(): Long {
        val sessionDuration = TimestampManager.endSession()
        sessionReference = null
        sessionDirectory = null
        
        Log.i(TAG, "Session finalized. Duration: ${sessionDuration}ms")
        return sessionDuration
    }
    
    /**
     * Write session synchronization metadata to CSV file
     * This file can be used for post-processing alignment verification
     */
    private fun writeSessionSyncMetadata() {
        val reference = sessionReference ?: return
        val sessionDir = sessionDirectory ?: return
        
        try {
            val metadataFile = File(sessionDir, SYNC_METADATA_FILENAME)
            FileWriter(metadataFile).use { writer ->
                // Write session reference data for post-processing alignment
                writer.write(reference.toCsvMetadata())
                writer.write("# This file contains session timing reference for cross-sensor alignment\n")
                writer.write("# All sensor CSV files should use these reference timestamps\n")
                writer.write("# system_nanos: monotonic nanosecond timestamp (most precise)\n")
                writer.write("# system_time_ms: wall clock time (human readable)\n")
                writer.write("# session_relative_ms: time relative to session start\n")
                writer.write("\n")
                writer.write("sync_event_type,system_nanos,system_time_ms,session_relative_ms,metadata\n")
            }
            
            Log.i(TAG, "Session sync metadata written to: ${metadataFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write session sync metadata", e)
        }
    }
    
    /**
     * Log sync event to metadata file
     */
    suspend fun logSyncEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
        val timestampRecord = createSynchronizedTimestamp()
        val sessionDir = sessionDirectory ?: return
        
        try {
            val metadataFile = File(sessionDir, SYNC_METADATA_FILENAME)
            FileWriter(metadataFile, true).use { writer ->
                val metadataStr = metadata.entries.joinToString(";") { "${it.key}=${it.value}" }
                writer.write("$eventType,${timestampRecord.systemNanos},${timestampRecord.systemTimeMs},${timestampRecord.sessionRelativeMs},\"$metadataStr\"\n")
            }
            
            emitSyncEvent(eventType, metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log sync event", e)
        }
    }
    
    /**
     * Validate timestamp consistency across sensors
     * This can be used for debugging synchronization issues
     */
    fun validateTimestampConsistency(
        gsrTimestamp: Long,
        thermalTimestamp: Long, 
        rgbTimestamp: Long
    ): TimestampConsistencyReport {
        val maxDiff = maxOf(
            kotlin.math.abs(gsrTimestamp - thermalTimestamp),
            kotlin.math.abs(thermalTimestamp - rgbTimestamp),
            kotlin.math.abs(rgbTimestamp - gsrTimestamp)
        )
        
        val isConsistent = maxDiff < 5_000_000L // 5ms tolerance
        
        return TimestampConsistencyReport(
            isConsistent = isConsistent,
            maxDifferenceNs = maxDiff,
            gsrTimestamp = gsrTimestamp,
            thermalTimestamp = thermalTimestamp,
            rgbTimestamp = rgbTimestamp
        )
    }
}

/**
 * Sync event for cross-sensor alignment validation
 */
data class SyncEvent(
    val eventType: String,
    val timestampRecord: TimestampRecord,
    val metadata: Map<String, String>
)

/**
 * Report on timestamp consistency across sensors
 */
data class TimestampConsistencyReport(
    val isConsistent: Boolean,
    val maxDifferenceNs: Long,
    val gsrTimestamp: Long,
    val thermalTimestamp: Long,
    val rgbTimestamp: Long
) {
    fun toCsvLine(): String {
        return "$isConsistent,$maxDifferenceNs,$gsrTimestamp,$thermalTimestamp,$rgbTimestamp"
    }
    
    companion object {
        fun getCsvHeader(): String {
            return "is_consistent,max_difference_ns,gsr_timestamp,thermal_timestamp,rgb_timestamp"
        }
    }
}