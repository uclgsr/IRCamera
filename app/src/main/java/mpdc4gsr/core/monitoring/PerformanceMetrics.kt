package mpdc4gsr.core.monitoring

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object PerformanceMetrics {
    private const val TAG = "PerformanceMetrics"
    private val startTimes = ConcurrentHashMap<String, Long>()
    private val metrics = ConcurrentHashMap<String, AtomicLong>()

    fun initialize() {
        val appStartTime = SystemClock.elapsedRealtime()
        startTimes["app_cold_start"] = appStartTime
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

    fun logMetric(
        metricName: String,
        value: Long,
    ) {
        // TODO: Send to analytics backend
    }

    fun incrementCounter(counterName: String) {
        metrics.getOrPut(counterName) { AtomicLong(0) }.incrementAndGet()
    }

    fun getCounter(counterName: String): Long = metrics[counterName]?.get() ?: 0

    fun recordColdStartComplete() {
        val duration = endMeasurement("app_cold_start")
        if (duration > 0) {
            if (duration > 2000) {
            }
        }
    }

    fun recordFrameTime(frameTimeNanos: Long) {
        val frameTimeMs = frameTimeNanos / 1_000_000
        if (frameTimeMs > 16) {
            incrementCounter("janky_frames")
            if (frameTimeMs > 32) {
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
    }

    fun reset() {
        startTimes.clear()
        metrics.clear()
    }
}

inline fun <T> measureTime(
    operationName: String,
    block: () -> T,
): T {
    PerformanceMetrics.startMeasurement(operationName)
    try {
        return block()
    } finally {
        PerformanceMetrics.endMeasurement(operationName)
    }
}

suspend inline fun <T> measureTimeSuspend(
    operationName: String,
    crossinline block: suspend () -> T,
): T {
    PerformanceMetrics.startMeasurement(operationName)
    try {
        return block()
    } finally {
        PerformanceMetrics.endMeasurement(operationName)
    }
}
