package mpdc4gsr.core.hardware.gsr.model

data class GSRSample(
    val timestamp: Long,
    val timestampIso: String,
    val gsrMicrosiemens: Double,
    val gsrRaw: Int,
    val ppgRaw: Int = 0,
    val qualityScore: Double,
    val connectionRssi: Int,
) {
    val isValid: Boolean
        get() =
            gsrRaw in 0..4095 &&
                gsrMicrosiemens > 0.0 &&
                qualityScore >= 0.5
    val resistanceOhms: Double
        get() = if (gsrMicrosiemens > 0) 1_000_000.0 / gsrMicrosiemens else Double.MAX_VALUE
    val qualityLevel: QualityLevel
        get() =
            when {
                qualityScore >= 0.9 -> QualityLevel.EXCELLENT
                qualityScore >= 0.7 -> QualityLevel.GOOD
                qualityScore >= 0.5 -> QualityLevel.FAIR
                else -> QualityLevel.POOR
            }

    fun toCsvRow(): String = "$timestamp,$timestampIso,$gsrMicrosiemens,$gsrRaw,$ppgRaw,$qualityScore,$connectionRssi"

    fun toMap(): Map<String, Any> =
        mapOf(
            "timestamp" to timestamp,
            "timestamp_iso" to timestampIso,
            "gsr_microsiemens" to gsrMicrosiemens,
            "gsr_raw" to gsrRaw,
            "ppg_raw" to ppgRaw,
            "quality_score" to qualityScore,
            "connection_rssi" to connectionRssi,
            "resistance_ohms" to resistanceOhms,
            "is_valid" to isValid,
            "quality_level" to qualityLevel.name,
        )

    enum class QualityLevel {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
    }

    companion object {
        const val CSV_HEADER =
            "timestamp_ns,timestamp_iso,gsr_microsiemens,gsr_raw,ppg_raw,quality_score,connection_rssi"

        fun fromRawData(
            timestamp: Long,
            timestampIso: String,
            gsrCalibratedValue: Double,
            gsrRawValue: Int,
            ppgRawValue: Int = 0,
            connectionRssi: Int = -50,
        ): GSRSample {
            val qualityScore =
                when {
                    gsrRawValue < 0 || gsrRawValue > 4095 -> 0.0
                    gsrCalibratedValue <= 0 -> 0.3
                    gsrRawValue < 50 || gsrRawValue > 4000 -> 0.6
                    else -> 0.9
                }
            return GSRSample(
                timestamp = timestamp,
                timestampIso = timestampIso,
                gsrMicrosiemens = gsrCalibratedValue,
                gsrRaw = gsrRawValue,
                ppgRaw = ppgRawValue,
                qualityScore = qualityScore,
                connectionRssi = connectionRssi,
            )
        }

        fun fromCsvRow(csvRow: String): GSRSample? =
            try {
                val parts = csvRow.split(",")
                if (parts.size >= 7) {
                    GSRSample(
                        timestamp = parts[0].toLong(),
                        timestampIso = parts[1],
                        gsrMicrosiemens = parts[2].toDouble(),
                        gsrRaw = parts[3].toInt(),
                        ppgRaw = parts[4].toInt(),
                        qualityScore = parts[5].toDouble(),
                        connectionRssi = parts[6].toInt(),
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
    }
}
