package mpdc4gsr.core.data

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

class TimeSynchronizationService {
    companion object {
        private const val TAG = "TimeSynchronizationService"
        private const val SYNC_METADATA_FILENAME = "session_sync_metadata.csv"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionReference: SessionTimestampReference? = null
    private var sessionDirectory: String? = null
    private val _syncEvents = MutableSharedFlow<SyncEvent>()
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()
    fun initializeSession(sessionDirectory: String): SessionTimestampReference {
        this.sessionDirectory = sessionDirectory
        sessionReference = TimestampManager.startSession()
        AppLogger.i(TAG, "Session initialized with unified timestamp reference")
        writeSessionSyncMetadata()
        serviceScope.launch {
            logSessionStartSyncEvent()
        }
        return sessionReference!!
    }

    private suspend fun logSessionStartSyncEvent() {
        try {
            logSyncEvent(
                "SessionStart", mapOf(
                    "session_start_source" to "TimeSynchronizationService",
                    "unified_timestamp_system" to "enabled",
                    "cross_device_sync" to "available"
                )
            )
            AppLogger.i(TAG, "SessionStart sync event logged for cross-sensor alignment verification")
        } catch (e: java.io.IOException) {
            AppLogger.w(TAG, "Failed to log SessionStart sync event", e)
        }
    }

    fun getSessionReference(): SessionTimestampReference? = sessionReference
    fun createSynchronizedTimestamp(): TimestampRecord {
        return TimestampManager.createTimestampRecord()
    }

    fun convertDeviceTimestamp(deviceTimestamp: Long, sensorId: String): TimestampRecord {
        val unifiedTimestamp = TimestampManager.createTimestampRecord()
        Log.v(
            TAG,
            "Converted device timestamp for $sensorId: device=$deviceTimestamp, unified=${unifiedTimestamp.systemNanos}"
        )
        return unifiedTimestamp
    }

    suspend fun emitSyncEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
        val timestampRecord = createSynchronizedTimestamp()
        val syncEvent = SyncEvent(
            eventType = eventType,
            timestampRecord = timestampRecord,
            metadata = metadata
        )
        _syncEvents.emit(syncEvent)
        AppLogger.i(TAG, "Sync event emitted: $eventType at ${timestampRecord.systemTimeMs}ms")
    }

    fun finalizeSession(): Long {
        val sessionDuration = TimestampManager.endSession()
        sessionReference = null
        sessionDirectory = null
        AppLogger.i(TAG, "Session finalized. Duration: ${sessionDuration}ms")
        return sessionDuration
    }

    private fun writeSessionSyncMetadata() {
        val reference = sessionReference ?: return
        val sessionDir = sessionDirectory ?: return
        try {
            val metadataFile = File(sessionDir, SYNC_METADATA_FILENAME)
            FileWriter(metadataFile).use { writer ->
                writer.write(reference.toCsvMetadata())
                writer.write("# This file contains session timing reference for cross-sensor alignment\n")
                writer.write("# All sensor CSV files should use these reference timestamps\n")
                writer.write("# system_nanos: monotonic nanosecond timestamp (most precise)\n")
                writer.write("# system_time_ms: wall clock time (human readable)\n")
                writer.write("# session_relative_ms: time relative to session start\n")
                writer.write("\n")
                writer.write("sync_event_type,system_nanos,system_time_ms,session_relative_ms,metadata\n")
            }
            AppLogger.i(TAG, "Session sync metadata written to: ${metadataFile.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to write session sync metadata", e)
        }
    }

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
            AppLogger.e(TAG, "Failed to log sync event", e)
        }
    }

    suspend fun logTimestampWithDriftAnalysis(
        sensorId: String,
        deviceTimestamp: Long?,
        phoneTimestamp: Long = createSynchronizedTimestamp().systemNanos
    ) {
        try {
            val driftMetadata = mutableMapOf<String, String>()
            driftMetadata["sensor_id"] = sensorId
            driftMetadata["phone_timestamp_ns"] = phoneTimestamp.toString()
            deviceTimestamp?.let { deviceTs ->
                driftMetadata["device_timestamp_ns"] = deviceTs.toString()
                val driftNs = phoneTimestamp - deviceTs
                val driftMs = driftNs / 1_000_000.0
                driftMetadata["drift_ns"] = driftNs.toString()
                driftMetadata["drift_ms"] = String.format("%.3f", driftMs)
                AppLogger.v(TAG, "Timestamp drift analysis for $sensorId: ${driftMs}ms")
            } ?: run {
                driftMetadata["device_timestamp_ns"] = "unavailable"
                driftMetadata["drift_analysis"] = "no_device_timestamp"
            }
            logSyncEvent("DRIFT_ANALYSIS", driftMetadata)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to log drift analysis for $sensorId", e)
        }
    }

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
        val isConsistent = maxDiff < 5_000_000L
        return TimestampConsistencyReport(
            isConsistent = isConsistent,
            maxDifferenceNs = maxDiff,
            gsrTimestamp = gsrTimestamp,
            thermalTimestamp = thermalTimestamp,
            rgbTimestamp = rgbTimestamp
        )
    }
}

data class SyncEvent(
    val eventType: String,
    val timestampRecord: TimestampRecord,
    val metadata: Map<String, String>
)

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