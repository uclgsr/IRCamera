package com.topdon.tc001.sensors.shimmer.model

data class GSRSample(
    val timestampNanos: Long,
    val gsrMicrosiemens: Double,
    val rawADC12Bit: Int,  // **CRITICAL: Must be 0-4095 range for 12-bit ADC compliance**
    val resistanceOhms: Double,
    val qualityScore: Double,
    val connectionRSSI: Int,
    val sessionId: String
) {

    fun isValid(): Boolean {
        return rawADC12Bit in 0..4095 &&  // 12-bit ADC validation
                gsrMicrosiemens >= 0.0 &&
                resistanceOhms > 0.0 &&
                qualityScore in 0.0..1.0 &&
                timestampNanos > 0
    }

    fun getFormattedTimestamp(): String {
        val millis = timestampNanos / 1_000_000
        return java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date(millis))
    }

    fun getQualityLevel(): String = when {
        qualityScore >= 0.95 -> "Excellent"
        qualityScore >= 0.85 -> "Good"
        qualityScore >= 0.70 -> "Fair"
        qualityScore >= 0.50 -> "Poor"
        else -> "Critical"
    }

    fun toCsvRow(): String {
        return "$timestampNanos,$gsrMicrosiemens,$rawADC12Bit,$resistanceOhms,$qualityScore,$connectionRSSI"
    }

    fun withQuality(newQualityScore: Double): GSRSample {
        return copy(qualityScore = newQualityScore.coerceIn(0.0, 1.0))
    }
}
