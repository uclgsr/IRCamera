package mpdc4gsr.feature.gsr.data

object GSRConstants {
    // GSR calculation constants based on Shimmer3 GSR hardware specifications
    const val ADC_MAX_VALUE = 4095.0
    const val REFERENCE_VOLTAGE = 3.0
    const val REFERENCE_RESISTANCE_OHMS = 40200.0
    const val VOLTAGE_DIVIDER = 1000.0
    const val MICROSIEMENS_CONVERSION = 1000000.0

    // Sampling rate configuration
    const val GSR_SAMPLING_RATE = 128.0

    // Signal quality thresholds
    const val GSR_RAW_LOWER_BOUND = 100
    const val GSR_RAW_UPPER_BOUND = 4000
    const val GSR_MICROSIEMENS_LOWER_BOUND = 0.1
    const val GSR_MICROSIEMENS_UPPER_BOUND = 100.0
    const val GSR_HIGH_THRESHOLD = 50.0
    const val GSR_LOW_THRESHOLD = 0.5

    // Connection health monitoring constants
    const val TIMING_HEALTH_ACCEPTABLE_MS = 1000L
    const val HEALTH_SCORE_WEIGHT_HISTORICAL = 0.8
    const val HEALTH_SCORE_WEIGHT_SAMPLE = 0.1
    const val HEALTH_SCORE_WEIGHT_TIMING = 0.1
    const val POOR_CONNECTION_THRESHOLD = 30.0

    // GSR range limits for uncalibrated values
    const val GSR_UNCAL_LIMIT_LOW = 100
    const val GSR_UNCAL_LIMIT_HIGH = 4000
}
