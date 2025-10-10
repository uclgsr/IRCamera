package mpdc4gsr.feature.connectivity.data

object RecordingConstants {
    // Storage estimation constants
    const val FALLBACK_AVAILABLE_SPACE_GB = 10.0
    const val RGB_STORAGE_MB_PER_MIN = 50.0
    const val THERMAL_STORAGE_MB_PER_MIN = 5.0
    const val SHIMMER_STORAGE_MB_PER_MIN = 1.0
    const val MIN_STORAGE_SPACE_GB = 1.0

    // Timing constants
    const val SYNC_MARKER_DISTRIBUTION_DELAY_MS = 50L
    const val STATUS_UPDATE_INTERVAL_MS = 1000L
    const val ERROR_RECOVERY_DELAY_MS = 2000L
    const val VALIDATION_TIMEOUT_MS = 30000L
    const val SYNC_ACCURACY_THRESHOLD_MS = 5L
    const val MIN_RECORDING_DURATION_MS = 60000L
    const val BATTERY_OPTIMIZATION_CHECK_INTERVAL_MS = 5000L
}
