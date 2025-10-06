package mpdc4gsr.core.monitoring

import android.os.SystemClock
import mpdc4gsr.core.utils.AppLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance metrics tracking for KPIs.
 *
 * Key Performance Indicators:
 * - Cold start time: Target < 2 seconds
 * - Crash-free sessions: Target > 99.5%
 * - ANR rate: Target < 0.1%
 * - Janky frames: Target < 5%
 * - Battery impact: Target < 5%/hour during recording
 *
 * Integration:
 * - Firebase Performance Monitoring
 * - Custom analytics
 * - Internal metrics dashboard
 */
object PerformanceMetrics {

    private const val TAG = "PerformanceMetrics"

    private val startTimes = ConcurrentHashMap<String, Long>()
    private val metrics = ConcurrentHashMap<String, AtomicLong>()

    /**
     * Initialize performance tracking.
     * Call this as early as possible in Application.onCreate().
     */
    fun initialize() {
        val appStartTime = SystemClock.elapsedRealtime()
        startTimes["app_cold_start"] = appStartTime
        AppLogger.d(TAG, "Performance metrics initialized")
    }

    /**
     * Start measuring an operation.
     *
     * @param operationName Unique identifier for the operation
     */
    fun startMeasurement(operationName: String) {
        startTimes[operationName] = SystemClock.elapsedRealtime()
    }

    /**
     * End measurement and log the duration.
     *
     * @param operationName Unique identifier for the operation
     * @return Duration in milliseconds, or -1 if measurement was not started
     */
    fun endMeasurement(operationName: String): Long {
        val startTime = startTimes.remove(operationName) ?: return -1
        val duration = SystemClock.elapsedRealtime() - startTime

        logMetric(operationName, duration)
        return duration
    }

    /**
     * Log a metric value.
     *
     * @param metricName Name of the metric
     * @param value Metric value
     */
    fun logMetric(metricName: String, value: Long) {
        AppLogger.i(TAG, "Metric: $metricName = $value ms")

        // TODO: Send to analytics backend
    }

    /**
     * Increment a counter metric.
     *
     * @param counterName Name of the counter
     */
    fun incrementCounter(counterName: String) {
        metrics.getOrPut(counterName) { AtomicLong(0) }.incrementAndGet()
    }

    /**
     * Get counter value.
     *
     * @param counterName Name of the counter
     * @return Current counter value
     */
    fun getCounter(counterName: String): Long {
        return metrics[counterName]?.get() ?: 0
    }

    /**
     * Record cold start completion.
     * Call this when the first frame is rendered.
     */
    fun recordColdStartComplete() {
        val duration = endMeasurement("app_cold_start")
        if (duration > 0) {
            AppLogger.i(TAG, "Cold start completed in $duration ms (target: < 2000 ms)")

            if (duration > 2000) {
                AppLogger.w(TAG, "Cold start exceeded target of 2000ms")
            }
        }
    }

    /**
     * Record frame rendering time.
     * Use with FrameMetrics API to track jank.
     *
     * @param frameTimeNanos Frame rendering time in nanoseconds
     */
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

    /**
     * Get janky frame percentage.
     *
     * @return Percentage of janky frames (0-100)
     */
    fun getJankyFramePercentage(): Float {
        val totalFrames = getCounter("total_frames")
        if (totalFrames == 0L) return 0f

        val jankyFrames = getCounter("janky_frames")
        return (jankyFrames.toFloat() / totalFrames) * 100
    }

    /**
     * Log current metrics summary.
     */
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

    /**
     * Reset all metrics.
     * Useful for testing or starting fresh measurements.
     */
    fun reset() {
        startTimes.clear()
        metrics.clear()
        AppLogger.d(TAG, "All metrics reset")
    }
}

/**
 * Inline function to measure the execution time of a block of code.
 *
 * Usage:
 * ```
 * measureTime("database_query") {
 *     database.query(...)
 * }
 * ```
 */
inline fun <T> measureTime(operationName: String, block: () -> T): T {
    PerformanceMetrics.startMeasurement(operationName)
    try {
        return block()
    } finally {
        PerformanceMetrics.endMeasurement(operationName)
    }
}

/**
 * Suspend version of measureTime for coroutines.
 *
 * Usage:
 * ```
 * measureTimeSuspend("network_request") {
 *     apiService.fetchData()
 * }
 * ```
 */
suspend inline fun <T> measureTimeSuspend(operationName: String, crossinline block: suspend () -> T): T {
    PerformanceMetrics.startMeasurement(operationName)
    try {
        return block()
    } finally {
        PerformanceMetrics.endMeasurement(operationName)
    }
}
