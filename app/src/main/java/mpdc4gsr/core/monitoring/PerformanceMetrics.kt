package mpdc4gsr.core.monitoring

import android.os.SystemClock
import mpdc4gsr.core.utils.AppLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object PerformanceMetrics {
    private const val TAG = "PerformanceMetrics"
    private val startTimes = ConcurrentHashMap<String, Long>()
    private val metrics = ConcurrentHashMap<String, AtomicLong>()

    fun initialize() {
        val appStartTime = SystemClock.elapsedRealtime()
        startTimes["app_cold_start"] = appStartTime
        AppLogger.d(TAG, "Performance metrics initialized")
    }

    fun startMeasurement(operationName: String) {
        startTimes[operationName] = SystemClock.elapsedRealtime()
    }

    fun endMeasurement(operationName: String): Long {
        val startTime = startTimes.remove(operationName) ?: return -1
        val duration = SystemClock.elapsedRealtime() - startTime
        logMetric(operationName, duration)
        return duration
    }

    fun logMetric(metricName: String, value: Long) {
        AppLogger.i(TAG, "Metric: $metricName = $value ms")
        // TODO: Send to analytics backend
    }

    fun incrementCounter(counterName: String) {
        metrics.getOrPut(counterName) { AtomicLong(0) }.incrementAndGet()
    }

    fun getCounter(counterName: String): Long {
        return metrics[counterName]?.get() ?: 0
    }

    fun recordColdStartComplete() {
        val duration = endMeasurement("app_cold_start")
        if (duration > 0) {
            AppLogger.i(TAG, "Cold start completed in $duration ms (target: < 2000 ms)")
            if (duration > 2000) {
                AppLogger.w(TAG, "Cold start exceeded target of 2000ms")
            }
        }
    }

    fun recordFrameTime(frameTimeNanos: Long) {
        val frameTimeMs = frameTimeNanos / 1_000_000
        if (frameTimeMs > 16) {
            incrementCounter("janky_frames")
            if (frameTimeMs > 32) {
                AppLogger.w(TAG, "Severe frame jank detected: ${frameTimeMs}ms")
            }
        }
        incrementCounter("total_frames")
    }

    fun getJankyFramePercentage(): Float {
        val totalFrames = getCounter("total_frames")
        if (totalFrames == 0L) return 0f
        val jankyFrames = getCounter("janky_frames")
        return (jankyFrames.toFloat() / totalFrames) * 100
    }

    fun logSummary() {
        AppLogger.i(TAG, "=== Performance Metrics Summary ===")
        AppLogger.i(TAG, "Total frames: ${getCounter("total_frames")}")
        AppLogger.i(
            TAG,
            "Janky frames: ${getCounter("janky_frames")} (${String.format("%.2f", getJankyFramePercentage())}%)"
        )
        AppLogger.i(TAG, "Recording sessions: ${getCounter("recording_sessions")}")
        AppLogger.i(TAG, "Successful recordings: ${getCounter("successful_recordings")}")
        AppLogger.i(TAG, "Failed recordings: ${getCounter("failed_recordings")}")
        AppLogger.i(TAG, "===================================")
    }

    fun reset() {
        startTimes.clear()
        metrics.clear()
        AppLogger.d(TAG, "All metrics reset")
    }
}

inline fun <T> measureTime(operationName: String, block: () -> T): T {
    PerformanceMetrics.startMeasurement(operationName)
    try {
        return block()
    } finally {
        PerformanceMetrics.endMeasurement(operationName)
    }
}

suspend inline fun <T> measureTimeSuspend(operationName: String, crossinline block: suspend () -> T): T {
    PerformanceMetrics.startMeasurement(operationName)
    try {
        return block()
    } finally {
        PerformanceMetrics.endMeasurement(operationName)
    }
}
