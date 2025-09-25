package com.mpdc4gsr.gsr.model

data class GSRSample(
    val timestamp: Long,
    val utcTimestamp: Long = timestamp,
    val conductance: Double,
    val resistance: Double,
    val rawValue: Int = 0,
    val sampleIndex: Long = 0,
    val sessionId: String,
    // Additional properties for app module compatibility
    val timestampIso: String = "",
    val ppgRaw: Int = 0,
    val qualityScore: Double = 1.0,
    val connectionRssi: Int = -50
) {
    // Computed properties for backward compatibility with app module
    val gsrMicrosiemens: Double get() = conductance
    val gsrRaw: Int get() = rawValue
    val resistanceOhms: Double get() = resistance * 1000.0 // Convert from kOhms to Ohms
    
    val isValid: Boolean
        get() = gsrRaw in 0..4095 &&
                gsrMicrosiemens > 0.0 &&
                qualityScore >= 0.5

    val qualityLevel: QualityLevel
        get() = when {
            qualityScore >= 0.9 -> QualityLevel.EXCELLENT
            qualityScore >= 0.7 -> QualityLevel.GOOD
            qualityScore >= 0.5 -> QualityLevel.FAIR
            else -> QualityLevel.POOR
        }

    enum class QualityLevel {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR
    }

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
        
        // Factory method for creating GSRSample with app module style parameters
        fun create(
            timestamp: Long,
            timestampIso: String,
            gsrMicrosiemens: Double,
            gsrRaw: Int,
            ppgRaw: Int = 0,
            qualityScore: Double,
            connectionRssi: Int = -50
        ): GSRSample {
            return GSRSample(
                timestamp = timestamp,
                utcTimestamp = timestamp,
                conductance = gsrMicrosiemens,
                resistance = if (gsrMicrosiemens > 0) 1_000_000.0 / gsrMicrosiemens / 1000.0 else 1000.0, // Convert to kOhms
                rawValue = gsrRaw,
                sampleIndex = System.nanoTime(),
                sessionId = "session_${timestamp}",
                timestampIso = timestampIso,
                ppgRaw = ppgRaw,
                qualityScore = qualityScore,
                connectionRssi = connectionRssi
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
