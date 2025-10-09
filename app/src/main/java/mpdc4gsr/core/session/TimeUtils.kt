package mpdc4gsr.core.session

import android.os.SystemClock
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            Log.d(TAG, "Ground truth timestamp initialised: $deviceGroundTruthBase")
            Log.d(TAG, "Device model: $deviceModel, processor: $detectedProcessor")
            Log.d(TAG, "Boot reference: $bootTimeReference")
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
            Log.d(TAG, "PC time offset set to: ${offset}ms")
        } catch (ignored: Exception) {
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

    fun getSynchronizedTimestamp(): Long = getUtcTimestamp()

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
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    fun generateSessionId(prefix: String = "GSR"): String {
        return try {
            val timestamp =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(Date(getSynchronizedTimestamp()))
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
        )
    }

    fun getMonotonicTimestampNs(): Long {
        return try {
            SystemClock.elapsedRealtimeNanos()
        } catch (e: Exception) {
            System.nanoTime()
        }
    }

    fun getMonotonicTimestampMs(): Long = getMonotonicTimestampNs() / 1_000_000L

    fun getElapsedTimeMs(startMonotonicNs: Long): Long {
        return (getMonotonicTimestampNs() - startMonotonicNs) / 1_000_000L
    }
}
