package com.topdon.tc001.sensors.shimmer.model

enum class ConnectionQuality(
    val displayName: String,
    val description: String,
    val minScore: Double,
    val color: String  // For UI display
) {

    EXCELLENT(
        displayName = "Excellent",
        description = "Research-grade quality, minimal noise",
        minScore = 0.95,
        color = "#4CAF50"  // Green
    ),

    GOOD(
        displayName = "Good",
        description = "High quality, suitable for research",
        minScore = 0.85,
        color = "#8BC34A"  // Light Green
    ),

    FAIR(
        displayName = "Fair",
        description = "Acceptable quality with minor issues",
        minScore = 0.70,
        color = "#FF9800"  // Orange
    ),

    POOR(
        displayName = "Poor",
        description = "Poor quality, data reliability affected",
        minScore = 0.50,
        color = "#FF5722"  // Deep Orange
    ),

    CRITICAL(
        displayName = "Critical",
        description = "Critical issues, data unreliable",
        minScore = 0.0,
        color = "#F44336"  // Red
    ),

    UNKNOWN(
        displayName = "Unknown",
        description = "Quality assessment unavailable",
        minScore = 0.0,
        color = "#9E9E9E"  // Grey
    );

    companion object {

        fun fromScore(score: Double): ConnectionQuality = when {
            score >= EXCELLENT.minScore -> EXCELLENT
            score >= GOOD.minScore -> GOOD
            score >= FAIR.minScore -> FAIR
            score >= POOR.minScore -> POOR
            score >= 0.0 -> CRITICAL
            else -> UNKNOWN
        }

        fun fromRSSI(rssi: Int): ConnectionQuality = when {
            rssi >= -50 -> EXCELLENT
            rssi >= -60 -> GOOD
            rssi >= -70 -> FAIR
            rssi >= -80 -> POOR
            rssi > -100 -> CRITICAL
            else -> UNKNOWN
        }

        fun getMinimumResearchQuality(): ConnectionQuality = FAIR

        fun isResearchGrade(quality: ConnectionQuality): Boolean {
            return quality.minScore >= getMinimumResearchQuality().minScore
        }
    }

    fun isAcceptableForResearch(): Boolean {
        return minScore >= FAIR.minScore
    }

    fun toPercentage(): Int {
        return (minScore * 100).toInt()
    }

    fun getRecommendation(): String = when (this) {
        EXCELLENT -> "Optimal for all research applications"
        GOOD -> "Suitable for most research protocols"
        FAIR -> "Consider improving signal strength"
        POOR -> "Move closer to device or check interference"
        CRITICAL -> "Connection too unstable for reliable data"
        UNKNOWN -> "Establishing connection quality..."
    }
}
