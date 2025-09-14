package com.shimmerresearch.driver

import kotlin.math.pow


object CalibrationUtilities {
    // GSR calibration constants for different ranges
    private val GSR_UNCALIBRATED_TO_KOHMS =
    doubleArrayOf(
    6.8129, // Range 0: 10kΩ - 56kΩ
    27.2518, // Range 1: 56kΩ - 220kΩ
    108.2072, // Range 2: 220kΩ - 680kΩ
    351.8677, // Range 3: 680kΩ - 4.7MΩ
    )

    private val GSR_OFFSET =
    doubleArrayOf(
    2294.5, // Range 0 offset
    2617.0, // Range 1 offset
    2808.8, // Range 2 offset
    3012.3, // Range 3 offset
    )


    fun calibrateGSRToResistance(
    uncalibratedData: Int,
    gsrRange: Int,
    ): Double {
    if (gsrRange < 0 || gsrRange > 3) return 0.0

    val offset = GSR_OFFSET[gsrRange]
    val multiplier = GSR_UNCALIBRATED_TO_KOHMS[gsrRange]

    // Apply calibration formula: R = multiplier / (ADC - offset)
    val denominator = uncalibratedData - offset
    return if (denominator > 0) multiplier / denominator else 0.0
    }


    fun resistanceToConductance(resistanceKohms: Double): Double {
    return if (resistanceKohms > 0) 1000.0 / resistanceKohms else 0.0
    }


    fun conductanceToResistance(conductanceUS: Double): Double {
    return if (conductanceUS > 0) 1000.0 / conductanceUS else 0.0
    }


    fun selectOptimalGSRRange(resistanceKohms: Double): Int {
    return when {
    resistanceKohms <= 56 -> Configuration.GSR_RANGE_10KOHM_56KOHM
    resistanceKohms <= 220 -> Configuration.GSR_RANGE_56KOHM_220KOHM
    resistanceKohms <= 680 -> Configuration.GSR_RANGE_220KOHM_680KOHM
    resistanceKohms <= 4700 -> Configuration.GSR_RANGE_680KOHM_4_7MOHM
    else -> Configuration.GSR_RANGE_680KOHM_4_7MOHM
    }
    }


    fun generateSimulatedADC(
    targetResistanceKohms: Double,
    gsrRange: Int,
    ): Int {
    if (gsrRange < 0 || gsrRange > 3) return 2048

    val offset = GSR_OFFSET[gsrRange]
    val multiplier = GSR_UNCALIBRATED_TO_KOHMS[gsrRange]

    // Reverse calibration: ADC = (multiplier / R) + offset
    val calculatedADC = (multiplier / targetResistanceKohms) + offset

    // Add some realistic noise (±5%)
    val noise = (Math.random() - 0.5) * 0.1 * calculatedADC

    return (calculatedADC + noise).toInt().coerceIn(0, 4095) // 12-bit ADC
    }


    fun isValidGSRReading(
    resistanceKohms: Double,
    conductanceUS: Double,
    ): Boolean {
    // Physiologically reasonable ranges
    val validResistance = resistanceKohms >= 1.0 && resistanceKohms <= 10000.0
    val validConductance = conductanceUS >= 0.1 && conductanceUS <= 1000.0

    // Check consistency between resistance and conductance
    val calculatedConductance = resistanceToConductance(resistanceKohms)
    val consistency = kotlin.math.abs(calculatedConductance - conductanceUS) / conductanceUS < 0.1

    return validResistance && validConductance && consistency
    }


    fun applySmoothingFilter(
    values: DoubleArray,
    windowSize: Int = 5,
    ): DoubleArray {
    if (values.size < windowSize) return values

    val smoothed = DoubleArray(values.size)
    val halfWindow = windowSize / 2

    for (i in values.indices) {
    var sum = 0.0
    var count = 0

    val start = maxOf(0, i - halfWindow)
    val end = minOf(values.size - 1, i + halfWindow)

    for (j in start..end) {
    sum += values[j]
    count++
    }

    smoothed[i] = sum / count
    }

    return smoothed
    }


    data class GSRStatistics(
    val mean: Double,
    val standardDeviation: Double,
    val range: Double,
    val tonic: Double, // Baseline level
    val phasic: Double, // Response amplitude
    val isValidSignal: Boolean,
    )

    fun calculateGSRStatistics(conductanceValues: DoubleArray): GSRStatistics {
    if (conductanceValues.isEmpty()) {
    return GSRStatistics(0.0, 0.0, 0.0, 0.0, 0.0, false)
    }

    val mean = conductanceValues.average()
    val variance = conductanceValues.map { (it - mean).pow(2) }.average()
    val stdDev = kotlin.math.sqrt(variance)
    val range = conductanceValues.maxOrNull()!! - conductanceValues.minOrNull()!!

    // Estimate tonic and phasic components
    val smoothed = applySmoothingFilter(conductanceValues, 10)
    val tonic = smoothed.average()
    val phasic = stdDev * 2.0 // Approximate phasic activity

    // Signal quality assessment
    val isValid = mean >= 1.0 && mean <= 100.0 && stdDev < mean * 0.5 && range < mean * 2.0

    return GSRStatistics(mean, stdDev, range, tonic, phasic, isValid)
    }
}
