package mpdc4gsr.sensors.gsr

import kotlin.math.max

object GSRCalculationUtils {
    
    /**
     * Calculate GSR in microsiemens from raw ADC value.
     *
     * This method uses the standard Shimmer formula for converting a raw ADC value to GSR (microsiemens).
     * It includes input validation and bounds checking.
     *
     * @param rawValue Raw ADC value from the GSR sensor.
     * @return GSR value in microsiemens, or 0.0 if input is out of bounds or calculation fails.
     */
    fun calculateGSRMicrosiemens(rawValue: Int): Double {
        if (rawValue < GSRConstants.GSR_UNCAL_LIMIT_LOW || rawValue > GSRConstants.GSR_UNCAL_LIMIT_HIGH) {
            return 0.0
        }
        return try {
            val voltage = (rawValue / GSRConstants.ADC_MAX_VALUE) * GSRConstants.REFERENCE_VOLTAGE
            val gsrResistance = GSRConstants.REFERENCE_RESISTANCE_OHMS * ((GSRConstants.REFERENCE_VOLTAGE / voltage) - 1.0)
            val conductance = if (gsrResistance > 0) {
                (1.0 / gsrResistance) * GSRConstants.MICROSIEMENS_CONVERSION
            } else {
                0.0
            }
            conductance.coerceIn(0.0, GSRConstants.GSR_MICROSIEMENS_UPPER_BOUND)
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Calculate resistance from GSR conductance value
     */
    fun calculateResistanceFromGSR(gsrMicrosiemens: Double): Double {
        return if (gsrMicrosiemens > 0) {
            GSRConstants.MICROSIEMENS_CONVERSION / gsrMicrosiemens
        } else {
            Double.MAX_VALUE
        }
    }
    
    /**
     * Calculate signal quality score based on GSR values and thresholds
     */
    fun calculateSignalQuality(gsrMicrosiemens: Double, rawValue: Int): Double {
        return when {
            rawValue !in GSRConstants.GSR_RAW_LOWER_BOUND..GSRConstants.GSR_RAW_UPPER_BOUND -> 0.2
            gsrMicrosiemens !in GSRConstants.GSR_MICROSIEMENS_LOWER_BOUND..GSRConstants.GSR_MICROSIEMENS_UPPER_BOUND -> 0.3
            gsrMicrosiemens > GSRConstants.GSR_HIGH_THRESHOLD -> 0.4
            gsrMicrosiemens < GSRConstants.GSR_LOW_THRESHOLD -> 0.5
            else -> 0.9
        }
    }
    
    /**
     * Enhanced quality score calculation based on GSR raw value.
     */
    fun calculateQualityScore(gsrRaw: Int): Double {
        val baseQuality = when {
            gsrRaw <= 0 -> 0.0
            gsrRaw < GSRConstants.GSR_RAW_LOWER_BOUND -> 0.3
            gsrRaw > GSRConstants.GSR_RAW_UPPER_BOUND -> 0.4
            else -> 0.8
        }
        
        return max(0.0, baseQuality).coerceAtMost(1.0)
    }
}