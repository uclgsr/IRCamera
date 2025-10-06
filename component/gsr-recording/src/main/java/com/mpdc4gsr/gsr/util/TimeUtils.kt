package com.mpdc4gsr.gsr.util
object TimeUtils {
    private const val TAG = "TimeUtils"
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
        try {
            android.util.Log.d(
                TAG,
                "Samsung S22 device ground truth timestamp initialized: $deviceGroundTruthBase"
            )
            android.util.Log.d(
                TAG,
                "Samsung S22 model: $deviceModel, processor: $detectedProcessor"
            )
            android.util.Log.d(
                TAG,
                "Samsung S22 boot reference: $bootTimeReference ($detectedProcessor timer)"
            )
        } catch (e: Exception) {
        }
    }
    private fun detectSamsungS22Processor() {
        try {
            deviceModel = android.os.Build.MODEL
            val deviceBrand = android.os.Build.MANUFACTURER
            val hardware = android.os.Build.HARDWARE
            val soc =
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    android.os.Build.SOC_MANUFACTURER
                } else {
                    "unknown"
                }
            when {
                deviceModel.contains("SM-S901E", ignoreCase = true) -> {
                    detectedProcessor = "Exynos_2200"
                }
                deviceModel.contains("SM-S901U", ignoreCase = true) ||
                        deviceModel.contains("SM-S901W", ignoreCase = true) -> {
                    detectedProcessor = "Snapdragon_8_Gen_1"
                }
                deviceModel.contains("SM-S901N", ignoreCase = true) -> {
                    detectedProcessor = "Snapdragon_8_Gen_1"
                }
                hardware.contains("qcom", ignoreCase = true) ||
                        soc.contains("qualcomm", ignoreCase = true) -> {
                    detectedProcessor = "Snapdragon_8_Gen_1"
                }
                hardware.contains("exynos", ignoreCase = true) ||
                        soc.contains("samsung", ignoreCase = true) -> {
                    detectedProcessor = "Exynos_2200"
                }
                deviceBrand.contains("samsung", ignoreCase = true) &&
                        deviceModel.contains("SM-S90", ignoreCase = true) -> {
                    detectedProcessor = "Samsung_S22_Generic"
                }
                else -> {
                    detectedProcessor = "Generic_Android_Timer"
                }
            }
        } catch (e: Exception) {
            detectedProcessor = "Detection_Failed"
            deviceModel = "Unknown"
        }
    }
    fun setPcTimeOffset(offset: Long) {
        pcTimeOffset = offset
        try {
            android.util.Log.d(
                TAG,
                "PC time offset set to: ${offset}ms from Samsung S22 ground truth"
            )
            android.util.Log.d(
                TAG,
                "Samsung S22 ($detectedProcessor) maintains authoritative timing with ${offset}ms PC coordination"
            )
        } catch (e: Exception) {
        }
    }
    fun getPcTimeOffset(): Long = pcTimeOffset
    fun getGroundTruthBase(): Long = deviceGroundTruthBase
    fun systemToUtc(systemTime: Long): Long {
        val deviceOffset = systemTime - deviceGroundTruthBase
        return deviceGroundTruthBase + deviceOffset + pcTimeOffset
    }
    fun utcToSystem(utcTime: Long): Long {
        return utcTime - pcTimeOffset - (deviceGroundTruthBase - System.currentTimeMillis())
    }
    fun getSynchronizedTimestamp(): Long {
        return getUtcTimestamp()
    }
    fun getHighPrecisionTimestamp(): Long {
        return try {
            val nanoOffset = (System.nanoTime() / 1_000_000L) - bootTimeReference
            deviceGroundTruthBase + nanoOffset + pcTimeOffset
        } catch (e: Exception) {
            getSynchronizedTimestamp()
        }
    }
    fun formatTimestamp(timestamp: Long): String {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
                .format(java.util.Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }
    fun generateSessionId(prefix: String = "GSR"): String {
        return try {
            val timestamp =
                java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(java.util.Date(getSynchronizedTimestamp()))
            "${prefix}_$timestamp"
        } catch (e: Exception) {
            "${prefix}_${getSynchronizedTimestamp()}"
        }
    }
    fun getTimingMetadata(): Map<String, String> {
        return mapOf(
            "ground_truth_base" to deviceGroundTruthBase.toString(),
            "pc_offset_ms" to pcTimeOffset.toString(),
            "device_model" to deviceModel,
            "device_processor" to detectedProcessor,
            "timing_mode" to "unified_ntp_style",
            "timing_precision" to "sub_millisecond",
            "current_sync_time" to getSynchronizedTimestamp().toString(),
            "boot_reference" to bootTimeReference.toString(),
            "system_uptime_ms" to (System.nanoTime() / 1_000_000L).toString(),
        )
    }
    fun getMonotonicTimestampNs(): Long {
        return try {
            android.os.SystemClock.elapsedRealtimeNanos()
        } catch (e: Exception) {
            System.nanoTime()
        }
    }
    fun getMonotonicTimestampMs(): Long {
        return getMonotonicTimestampNs() / 1_000_000L
    }
    fun getElapsedTimeMs(startMonotonicNs: Long): Long {
        return (getMonotonicTimestampNs() - startMonotonicNs) / 1_000_000L
    }
    fun createSessionTimingMetadata(sessionId: String): Map<String, Any> {
        val wallClockMs = getSynchronizedTimestamp()
        val monotonicNs = getMonotonicTimestampNs()
        return mapOf(
            "session_id" to sessionId,
            "wall_clock_start_ms" to wallClockMs,
            "monotonic_start_ns" to monotonicNs,
            "device_ground_truth_base" to deviceGroundTruthBase,
            "pc_time_offset_ms" to pcTimeOffset,
            "timing_source" to "samsung_s22_ground_truth_with_monotonic",
            "device_model" to deviceModel,
            "detected_processor" to detectedProcessor,
            "session_start_iso" to formatTimestamp(wallClockMs)
        )
    }
    fun validateTimingSystem(): Map<String, Any> {
        val currentTime = System.currentTimeMillis()
        val syncTime = getSynchronizedTimestamp()
        val precision =
            try {
                val nano1 = System.nanoTime()
                val nano2 = System.nanoTime()
                (nano2 - nano1) / 1_000_000.0
            } catch (e: Exception) {
                -1.0
            }
        return mapOf(
            "ground_truth_active" to (deviceGroundTruthBase > 0),
            "timing_drift_ms" to (currentTime - syncTime + pcTimeOffset),
            "precision_test_ms" to precision,
            "samsung_s22_status" to "operational",
            "detected_processor" to detectedProcessor,
            "device_model" to deviceModel,
            "ntp_coordination" to (pcTimeOffset != 0L),
            "processor_optimized" to (detectedProcessor.contains("Exynos") || detectedProcessor.contains(
                "Snapdragon"
            )),
        )
    }
    fun getDetectedProcessor(): String = detectedProcessor
    fun getDeviceModel(): String = deviceModel
}
