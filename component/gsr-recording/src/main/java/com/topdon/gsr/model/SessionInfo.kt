package com.topdon.gsr.model

/**
 * Session Information Model
 * Contains metadata and state for recording sessions
 */
data class SessionInfo(
    val sessionId: String,
    val startTime: Long,
    var endTime: Long? = null,
    var participantId: String? = null,
    var studyName: String? = null,
    var sampleCount: Long = 0,
    val metadata: MutableMap<String, String> = mutableMapOf(),
    val syncMarks: MutableList<SyncMark> = mutableListOf(),
    // Data availability flags for UI display
    var hasGSRData: Boolean = false,
    var hasRGBData: Boolean = false,
    var hasThermalData: Boolean = false,
) {
    /**
     * Check if session is currently active (not ended)
     */
    fun isActive(): Boolean = endTime == null

    /**
     * Get session duration in milliseconds
     * Returns current duration if active, total duration if completed
     */
    fun getDurationMs(): Long {
        val end = endTime ?: System.currentTimeMillis()
        return end - startTime
    }

    /**
     * Get formatted duration string
     */
    fun getDurationFormatted(): String {
        val durationMs = getDurationMs()
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Add synchronization mark
     */
    fun addSyncMark(mark: SyncMark) {
        syncMarks.add(mark)
    }

    /**
     * Get summary of available data types
     */
    fun getDataTypeSummary(): String {
        val types = mutableListOf<String>()
        if (hasGSRData) types.add("GSR")
        if (hasRGBData) types.add("RGB")
        if (hasThermalData) types.add("Thermal")

        return if (types.isEmpty()) "No data" else types.joinToString(", ")
    }
}
