package com.mpdc4gsr.libunified.app.utils

object UnifiedGsrUtils {
    private const val TAG = "UnifiedGsrUtils"

    // Time synchronization state
    private var pcTimeOffset: Long = 0L
    private var deviceGroundTruthBase: Long = System.currentTimeMillis()
    private var bootTimeReference: Long = 0L
    private var detectedProcessor: String = "Unknown"
    private var deviceModel: String = "Unknown"

    fun getUtcTimestamp(): Long {
        val currentDeviceTime = System.currentTimeMillis()
        val deviceOffset = currentDeviceTime - deviceGroundTruthBase
        return deviceGroundTruthBase + deviceOffset + pcTimeOffset
    }

    fun initializeGroundTruthTiming() {
        deviceGroundTruthBase = System.currentTimeMillis()
        detectSamsungS22Processor()
        try {
            bootTimeReference = System.nanoTime() / 1_000_000L
        } catch (e: Exception) {
            bootTimeReference = 0L
        }
    }

    fun setPcTimeOffset(offset: Long) {
        pcTimeOffset = offset
    }

    fun getPcTimeOffset(): Long = pcTimeOffset

    private fun detectSamsungS22Processor() {
        try {
            val model = android.os.Build.MODEL
            val processor = android.os.Build.HARDWARE
            deviceModel = model
            detectedProcessor = processor
            // Samsung S22 specific timing adjustments
            if (model.contains("SM-S9", ignoreCase = true) ||
                processor.contains("exynos", ignoreCase = true)
            ) {
                // Apply Samsung-specific timing corrections
                deviceGroundTruthBase += 5L // 5ms adjustment for Samsung timing
            }
        } catch (e: Exception) {
            detectedProcessor = "Detection Failed"
            deviceModel = "Unknown"
        }
    }

    data class DeviceTimingInfo(
        val processor: String,
        val model: String,
        val groundTruthBase: Long,
        val bootTimeReference: Long,
        val pcTimeOffset: Long,
    )

    fun getDeviceTimingInfo(): DeviceTimingInfo =
        DeviceTimingInfo(
            detectedProcessor,
            deviceModel,
            deviceGroundTruthBase,
            bootTimeReference,
            pcTimeOffset,
        )

    fun calculateGsrSampleTimestamp(
        sampleIndex: Long,
        samplingRate: Double,
    ): Long {
        val sampleTimeMs = (sampleIndex / samplingRate * 1000).toLong()
        return deviceGroundTruthBase + sampleTimeMs + pcTimeOffset
    }

    fun resistanceToMicrosiemens(resistance: Double): Double =
        if (resistance > 0) {
            1_000_000.0 / resistance
        } else {
            0.0
        }

    fun microsiemensToResistance(microsiemens: Double): Double =
        if (microsiemens > 0) {
            1_000_000.0 / microsiemens
        } else {
            Double.MAX_VALUE
        }

    fun applyGsrCalibration(
        rawValue: Double,
        gain: Double,
        offset: Double,
    ): Double = (rawValue * gain) + offset

    fun calculateBaseline(
        gsrValues: DoubleArray,
        windowSize: Int = 100,
    ): Double {
        if (gsrValues.isEmpty()) return 0.0
        val sortedValues = gsrValues.sorted()
        val baselineWindowSize = minOf(windowSize, sortedValues.size)
        return sortedValues.take(baselineWindowSize).average()
    }

    data class GsrPeak(
        val index: Int,
        val timestamp: Long,
        val value: Double,
        val amplitude: Double,
    )

    fun detectGsrPeaks(
        gsrValues: DoubleArray,
        timestamps: LongArray,
        threshold: Double = 0.1,
        minDistance: Int = 50,
    ): List<GsrPeak> {
        if (gsrValues.isEmpty() || gsrValues.size != timestamps.size) return emptyList()
        val peaks = mutableListOf<GsrPeak>()
        val baseline = calculateBaseline(gsrValues)
        var lastPeakIndex = -minDistance
        for (i in 1 until gsrValues.size - 1) {
            val current = gsrValues[i]
            val prev = gsrValues[i - 1]
            val next = gsrValues[i + 1]
            // Check if it's a local maximum
            if (current > prev && current > next) {
                val amplitude = current - baseline
                // Check if amplitude exceeds threshold and minimum distance
                if (amplitude > threshold && i - lastPeakIndex >= minDistance) {
                    peaks.add(GsrPeak(i, timestamps[i], current, amplitude))
                    lastPeakIndex = i
                }
            }
        }
        return peaks
    }

    fun smoothGsrData(
        gsrValues: DoubleArray,
        windowSize: Int = 5,
    ): DoubleArray {
        if (gsrValues.size <= windowSize) return gsrValues.copyOf()
        val smoothed = DoubleArray(gsrValues.size)
        val halfWindow = windowSize / 2
        for (i in gsrValues.indices) {
            val start = maxOf(0, i - halfWindow)
            val end = minOf(gsrValues.size - 1, i + halfWindow)
            var sum = 0.0
            var count = 0
            for (j in start..end) {
                sum += gsrValues[j]
                count++
            }
            smoothed[i] = sum / count
        }
        return smoothed
    }

    data class GsrStats(
        val mean: Double,
        val median: Double,
        val standardDeviation: Double,
        val min: Double,
        val max: Double,
        val range: Double,
        val peakCount: Int,
    )

    fun calculateGsrStats(
        gsrValues: DoubleArray,
        timestamps: LongArray,
    ): GsrStats {
        if (gsrValues.isEmpty()) {
            return GsrStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
        }
        val sorted = gsrValues.sorted()
        val mean = gsrValues.average()
        val median =
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2]
            }
        val variance = gsrValues.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        val min = sorted.first()
        val max = sorted.last()
        val range = max - min
        val peaks = detectGsrPeaks(gsrValues, timestamps)
        return GsrStats(mean, median, standardDeviation, min, max, range, peaks.size)
    }

    fun exportGsrToCsv(
        gsrValues: DoubleArray,
        timestamps: LongArray,
        samplingRate: Double,
    ): String {
        if (gsrValues.size != timestamps.size) {
            throw IllegalArgumentException("GSR values and timestamps must have same length")
        }
        val csv = StringBuilder()
        csv.appendLine("Index,Timestamp,GSR_Resistance,GSR_Microsiemens,Sample_Rate")
        for (i in gsrValues.indices) {
            val resistance = gsrValues[i]
            val microsiemens = resistanceToMicrosiemens(resistance)
            csv.appendLine("$i,${timestamps[i]},$resistance,$microsiemens,$samplingRate")
        }
        return csv.toString()
    }

    data class GsrQualityReport(
        val isValid: Boolean,
        val issues: List<String>,
        val qualityScore: Double,
    )

    fun validateGsrDataQuality(
        gsrValues: DoubleArray,
        samplingRate: Double,
    ): GsrQualityReport {
        val issues = mutableListOf<String>()
        var qualityScore = 1.0
        if (gsrValues.isEmpty()) {
            issues.add("No GSR data available")
            return GsrQualityReport(false, issues, 0.0)
        }
        // Check for invalid values
        val invalidCount = gsrValues.count { it <= 0 || it.isNaN() || it.isInfinite() }
        if (invalidCount > 0) {
            issues.add("$invalidCount invalid GSR values found")
            qualityScore -= (invalidCount.toDouble() / gsrValues.size) * 0.5
        }
        // Check sampling rate consistency
        if (samplingRate <= 0) {
            issues.add("Invalid sampling rate: $samplingRate")
            qualityScore -= 0.3
        }
        // Check for signal saturation
        val stats = calculateGsrStats(gsrValues, LongArray(gsrValues.size))
        if (stats.range < 0.001) {
            issues.add("GSR signal appears saturated (very low range)")
            qualityScore -= 0.4
        }
        // Check for excessive noise
        val smoothed = smoothGsrData(gsrValues)
        val noiseLevel =
            gsrValues
                .zip(smoothed)
                .map { (original, smooth) ->
                    kotlin.math.abs(original - smooth)
                }.average()
        if (noiseLevel > stats.standardDeviation * 0.5) {
            issues.add("High noise level detected")
            qualityScore -= 0.2
        }
        qualityScore = maxOf(0.0, qualityScore)
        return GsrQualityReport(
            isValid = issues.isEmpty() && qualityScore > 0.5,
            issues = issues,
            qualityScore = qualityScore,
        )
    }
}
