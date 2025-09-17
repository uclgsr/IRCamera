package com.topdon.tc001.data

import android.os.SystemClock
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.topdon.tc001.sensors.RecordingStats
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * SessionMetadata captures comprehensive timing information for multi-modal recording sessions
 * to enable precise post-processing synchronization as specified in the requirements.
 *
 * This class implements the common start time approach using both wall clock and monotonic
 * clock references to protect against system time adjustments during recording.
 */
data class SessionMetadata(
    val sessionId: String,

    // Wall clock timestamps (UTC milliseconds) - for human-readable reference
    val sessionStartTimestampMs: Long,
    val sessionEndTimestampMs: Long? = null,

    // Monotonic clock references (nanoseconds) - for interval measurements
    val sessionStartMonotonicNs: Long,
    val sessionEndMonotonicNs: Long? = null,

    // Human-readable timing information
    val sessionStartIso: String,
    val sessionEndIso: String? = null,

    // Device and timing information
    val deviceModel: String = android.os.Build.MODEL,
    val deviceManufacturer: String = android.os.Build.MANUFACTURER,
    val timingSource: String = "android_monotonic_realtime",

    // Modality file references
    val modalityFiles: MutableMap<String, String> = mutableMapOf(),

    // Sync event timestamps for alignment verification
    val syncEvents: MutableList<SyncEvent> = mutableListOf(),

    // Sensor lifecycle summaries keyed by logical sensor name
    val sensorSummaries: MutableMap<String, SensorSummary> = mutableMapOf(),

    // Stop results per sensor for quick lookup
    val stopResults: MutableMap<String, Boolean> = mutableMapOf(),

    // Session statistics
    val recordingDurationMs: Long? = null
) {

    companion object {
        private const val TAG = "SessionMetadata"

        /**
         * Creates a new session metadata with synchronized start time
         */
        fun createSessionStart(sessionId: String): SessionMetadata {
            val wallClockStartMs = System.currentTimeMillis()
            val monotonicStartNs = SystemClock.elapsedRealtimeNanos()
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            isoFormatter.timeZone = TimeZone.getTimeZone("UTC")

            return SessionMetadata(
                sessionId = sessionId,
                sessionStartTimestampMs = wallClockStartMs,
                sessionStartMonotonicNs = monotonicStartNs,
                sessionStartIso = isoFormatter.format(Date(wallClockStartMs))
            )
        }
    }

    /**
     * Marks the session as ended and calculates duration
     */
    fun markSessionEnd(): SessionMetadata {
        val wallClockEndMs = System.currentTimeMillis()
        val monotonicEndNs = SystemClock.elapsedRealtimeNanos()
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        isoFormatter.timeZone = TimeZone.getTimeZone("UTC")

        val durationMs = (monotonicEndNs - sessionStartMonotonicNs) / 1_000_000L

        return this.copy(
            sessionEndTimestampMs = wallClockEndMs,
            sessionEndMonotonicNs = monotonicEndNs,
            sessionEndIso = isoFormatter.format(Date(wallClockEndMs)),
            recordingDurationMs = durationMs
        )
    }

    /**
     * Adds a modality file reference with its start time relative to session start
     */
    fun addModalityFile(modalityType: String, fileName: String, startOffsetMs: Long = 0) {
        modalityFiles[modalityType] = fileName

        // Add sync event for modality start
        syncEvents.add(
            SyncEvent(
                eventType = "${modalityType}_START",
                timestampMs = sessionStartTimestampMs + startOffsetMs,
                monotonicOffsetNs = startOffsetMs * 1_000_000L,
                metadata = mapOf(
                    "modality" to modalityType,
                    "file" to fileName,
                    "offset_ms" to startOffsetMs.toString()
                )
            )
        )
    }

    /**
     * Adds a synchronization event for later alignment verification
     */
    fun addSyncEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
        val currentWallMs = System.currentTimeMillis()
        val currentMonotonicNs = SystemClock.elapsedRealtimeNanos()
        val offsetFromStartNs = currentMonotonicNs - sessionStartMonotonicNs

        syncEvents.add(
            SyncEvent(
                eventType = eventType,
                timestampMs = currentWallMs,
                monotonicOffsetNs = offsetFromStartNs,
                metadata = metadata
            )
        )
    }

    private fun relativeMillis(monotonicNs: Long): Long {
        return (monotonicNs - sessionStartMonotonicNs) / 1_000_000L
    }

    /**
     * Records that a sensor successfully started capturing data.
     */
    fun markSensorStart(
        sensorName: String,
        sensorId: String,
        sensorType: String,
        startMonotonicNs: Long,
        metadata: Map<String, String> = emptyMap()
    ) {
        val summary = SensorSummary(
            sensorId = sensorId,
            sensorType = sensorType,
            startTimestampNs = startMonotonicNs,
            startTimestampMs = monotonicToWallClock(startMonotonicNs),
            relativeStartMs = relativeMillis(startMonotonicNs)
        )
        summary.metadata.putAll(metadata)
        sensorSummaries[sensorName] = summary
    }

    /**
     * Updates the sensor summary with stop timing and performance data.
     */
    fun markSensorStop(
        sensorName: String,
        stopMonotonicNs: Long,
        success: Boolean,
        stats: RecordingStats? = null,
        metadata: Map<String, String> = emptyMap(),
        errorMessage: String? = null,
        sensorId: String? = null,
        sensorType: String? = null
    ) {
        val summary = sensorSummaries[sensorName] ?: SensorSummary(
            sensorId = sensorId ?: sensorName,
            sensorType = sensorType ?: "unknown",
            startTimestampNs = sessionStartMonotonicNs,
            startTimestampMs = sessionStartTimestampMs,
            relativeStartMs = 0L
        )

        summary.stopTimestampNs = stopMonotonicNs
        summary.stopTimestampMs = monotonicToWallClock(stopMonotonicNs)
        summary.relativeStopMs = relativeMillis(stopMonotonicNs)
        summary.status = if (success) "COMPLETED" else "FAILED"
        summary.metadata.putAll(metadata)

        stats?.let {
            summary.samplesRecorded = it.totalSamplesRecorded
            summary.averageDataRate = it.averageDataRate
            summary.droppedSamples = it.droppedSamples
            summary.syncMarkers = it.syncMarkersCount
            summary.storageUsedMb = it.storageUsedMB
        }

        if (!success && errorMessage != null) {
            summary.errors.add(errorMessage)
        }

        sensorSummaries[sensorName] = summary
    }

    /**
     * Stores the final stop results for each sensor.
     */
    fun recordStopResults(results: Map<String, Boolean>) {
        stopResults.clear()
        stopResults.putAll(results)
    }

    /**
     * Calculates timestamp relative to session start using monotonic clock
     */
    fun getRelativeTimestamp(): Long {
        val currentMonotonicNs = SystemClock.elapsedRealtimeNanos()
        return (currentMonotonicNs - sessionStartMonotonicNs) / 1_000_000L // Convert to milliseconds
    }

    /**
     * Converts a monotonic timestamp to wall clock time
     */
    fun monotonicToWallClock(monotonicNs: Long): Long {
        val offsetFromStartNs = monotonicNs - sessionStartMonotonicNs
        return sessionStartTimestampMs + (offsetFromStartNs / 1_000_000L)
    }

    /**
     * Saves session metadata to JSON file
     */
    fun saveToFile(sessionDirectory: File): File {
        val metadataFile = File(sessionDirectory, "session_metadata.json")
        val gson = GsonBuilder().setPrettyPrinting().create()

        try {
            metadataFile.writeText(gson.toJson(this))
            android.util.Log.i(TAG, "Session metadata saved: ${metadataFile.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save session metadata", e)
        }

        return metadataFile
    }

    /**
     * Creates timing header for CSV files
     */
    fun createTimingHeader(): String {
        return buildString {
            appendLine("# Multi-Modal Recording Session Timing Information")
            appendLine("# Session ID: $sessionId")
            appendLine("# Session Start: $sessionStartIso (${sessionStartTimestampMs}ms UTC)")
            appendLine("# Monotonic Start: ${sessionStartMonotonicNs}ns")
            appendLine("# Timing Source: $timingSource")
            appendLine("# Device: $deviceManufacturer $deviceModel")
            appendLine("#")
            appendLine("# Timestamps in this file are:")
            appendLine("#   - Wall clock: UTC milliseconds since epoch")
            appendLine("#   - Relative: milliseconds since session start (monotonic)")
            appendLine("#   - Monotonic: nanoseconds since boot (for interval calculation)")
            appendLine("#")
        }
    }
}

/**
 * Represents a synchronization event for cross-modal alignment
 */
data class SyncEvent(
    val eventType: String,
    val timestampMs: Long,
    val monotonicOffsetNs: Long,
    val metadata: Map<String, String> = emptyMap()
)

data class SensorSummary(
    val sensorId: String,
    val sensorType: String,
    val startTimestampNs: Long,
    val startTimestampMs: Long,
    val relativeStartMs: Long,
    var stopTimestampNs: Long? = null,
    var stopTimestampMs: Long? = null,
    var relativeStopMs: Long? = null,
    var status: String = "ACTIVE",
    val errors: MutableList<String> = mutableListOf(),
    var samplesRecorded: Long? = null,
    var averageDataRate: Double? = null,
    var droppedSamples: Long? = null,
    var syncMarkers: Int? = null,
    var storageUsedMb: Double? = null,
    val metadata: MutableMap<String, String> = mutableMapOf()
)
