package mpdc4gsr.core.session

import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.*

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
        detectDeviceProcessor()
        bootTimeReference =
            try {
                System.nanoTime() / 1_000_000L
            } catch (e: Exception) {
                0L
            }
        try {
        } catch (ignored: Exception) {
        }
    }

    private fun detectDeviceProcessor() {
        try {
            deviceModel = android.os.Build.MODEL
            val hardware = android.os.Build.HARDWARE
            val soc =
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    android.os.Build.SOC_MANUFACTURER
                } else {
                    "unknown"
                }
            detectedProcessor =
                when {
                    hardware.contains("qcom", ignoreCase = true) ||
                            soc.contains("qualcomm", ignoreCase = true) -> "Qualcomm"

                    hardware.contains("exynos", ignoreCase = true) ||
                            soc.contains("samsung", ignoreCase = true) -> "Exynos"

                    else -> "Generic_Android_Timer"
                }
        } catch (e: Exception) {
            detectedProcessor = "Detection_Failed"
            deviceModel = "Unknown"
        }
    }

    fun setPcTimeOffset(offset: Long) {
        pcTimeOffset = offset
        try {
        } catch (ignored: Exception) {
        }
    }

    fun getPcTimeOffset(): Long = pcTimeOffset

    fun getGroundTruthBase(): Long = deviceGroundTruthBase

    fun systemToUtc(systemTime: Long): Long {
        val deviceOffset = systemTime - deviceGroundTruthBase
        return deviceGroundTruthBase + deviceOffset + pcTimeOffset
    }

    fun utcToSystem(utcTime: Long): Long = utcTime - pcTimeOffset - (deviceGroundTruthBase - System.currentTimeMillis())

    fun getSynchronizedTimestamp(): Long = getUtcTimestamp()

    fun getHighPrecisionTimestamp(): Long =
        try {
            val nanoOffset = (System.nanoTime() / 1_000_000L) - bootTimeReference
            deviceGroundTruthBase + nanoOffset + pcTimeOffset
        } catch (e: Exception) {
            getSynchronizedTimestamp()
        }

    fun formatTimestamp(timestamp: Long): String =
        try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }

    fun generateSessionId(prefix: String = "GSR"): String =
        try {
            val timestamp =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(Date(getSynchronizedTimestamp()))
            "${prefix}_$timestamp"
        } catch (e: Exception) {
            "${prefix}_${getSynchronizedTimestamp()}"
        }

    fun getTimingMetadata(): Map<String, String> =
        mapOf(
            "ground_truth_base" to deviceGroundTruthBase.toString(),
            "pc_offset_ms" to pcTimeOffset.toString(),
            "device_model" to deviceModel,
            "device_processor" to detectedProcessor,
        )

    fun getMonotonicTimestampNs(): Long =
        try {
            SystemClock.elapsedRealtimeNanos()
        } catch (e: Exception) {
            System.nanoTime()
        }

    fun getMonotonicTimestampMs(): Long = getMonotonicTimestampNs() / 1_000_000L

    fun getElapsedTimeMs(startMonotonicNs: Long): Long = (getMonotonicTimestampNs() - startMonotonicNs) / 1_000_000L
}
