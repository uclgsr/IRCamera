package com.topdon.gsr.model

/**
 * Represents a single GSR data sample with timestamp information
 */
data class GSRSample(
    val timestamp: Long, // System timestamp in milliseconds
    val utcTimestamp: Long = timestamp, // UTC timestamp for synchronization (default to timestamp)
    val conductance: Double, // GSR conductance value in microsiemens
    val resistance: Double, // GSR resistance value in kilohms
    val rawValue: Int = 0, // Raw ADC value from sensor
    val sampleIndex: Long = 0, // Sequential sample index
    val sessionId: String, // Session identifier
) {
    companion object {
        /**
         * Create a simulated GSR sample for testing/demo purposes
         */
        fun createSimulated(
            timestamp: Long,
            utcTimestamp: Long,
            sampleIndex: Long,
            sessionId: String,
        ): GSRSample {
            // Simulate realistic GSR values with some variation
            val baseConductance = 10.0 // Base conductance in microsiemens
            val variation = Math.sin(sampleIndex * 0.1) * 2.0 + Math.random() * 1.0
            val conductance = baseConductance + variation
            val resistance = 1000.0 / conductance // Convert to kilohms
            val rawValue = (2048 + variation * 100).toInt() // Simulated ADC value

            return GSRSample(
                timestamp = timestamp,
                utcTimestamp = utcTimestamp,
                conductance = conductance,
                resistance = resistance,
                rawValue = rawValue,
                sampleIndex = sampleIndex,
                sessionId = sessionId,
            )
        }
    }

    /**
     * Convert sample to CSV row format
     */
    fun toCsvRow(): Array<String> {
        return arrayOf(
            timestamp.toString(),
            utcTimestamp.toString(),
            String.format("%.6f", conductance),
            String.format("%.6f", resistance),
            rawValue.toString(),
            sampleIndex.toString(),
            sessionId,
        )
    }
}

/**
 * Represents synchronization marks for cross-modal alignment
 */
data class SyncMark(
    val timestamp: Long,
    val utcTimestamp: Long,
    val eventType: String, // e.g., "THERMAL_CAPTURE", "SYNC_FLASH", "USER_TRIGGER"
    val sessionId: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun toCsvRow(): Array<String> {
        val metadataJson =
            if (metadata.isNotEmpty()) {
                metadata.entries.joinToString(";") { "${it.key}=${it.value}" }
            } else {
                ""
            }

        return arrayOf(
            timestamp.toString(),
            utcTimestamp.toString(),
            eventType,
            sessionId,
            metadataJson,
        )
    }
}
