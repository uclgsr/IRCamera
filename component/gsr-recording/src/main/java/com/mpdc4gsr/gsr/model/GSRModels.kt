package com.mpdc4gsr.gsr.model

data class GSRSample(
    val timestamp: Long,
    val utcTimestamp: Long = timestamp,
    val conductance: Double,
    val resistance: Double,
    val rawValue: Int = 0,
    val sampleIndex: Long = 0,
    val sessionId: String,
) {
    companion object {

        fun createSimulated(
            timestamp: Long,
            utcTimestamp: Long,
            sampleIndex: Long,
            sessionId: String,
        ): GSRSample {

            val baseConductance = 10.0
            val variation = Math.sin(sampleIndex * 0.1) * 2.0 + Math.random() * 1.0
            val conductance = baseConductance + variation
            val resistance = 1000.0 / conductance
            val rawValue = (2048 + variation * 100).toInt()

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

data class SyncMark(
    val timestamp: Long,
    val utcTimestamp: Long,
    val eventType: String,
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

// Legacy GSRSample class for backward compatibility with app module
// This matches the original GSRSample that was in mpdc4gsr.sensors.unified.model
data class LegacyGSRSample(
    val timestamp: Long,
    val timestampIso: String,
    val gsrMicrosiemens: Double,
    val gsrRaw: Int,
    val ppgRaw: Int = 0,
    val qualityScore: Double,
    val connectionRssi: Int
) {
    val isValid: Boolean
        get() = gsrRaw in 0..4095 &&
                gsrMicrosiemens > 0.0 &&
                qualityScore >= 0.5

    val resistanceOhms: Double
        get() = if (gsrMicrosiemens > 0) 1_000_000.0 / gsrMicrosiemens else Double.MAX_VALUE
}
