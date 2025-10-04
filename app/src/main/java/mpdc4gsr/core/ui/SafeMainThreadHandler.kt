package mpdc4gsr.core.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import java.util.concurrent.atomic.AtomicLong

/**
 * Safe main thread handler that prevents ANR by monitoring execution time.
 *
 * This handler wraps Runnables to detect if they take too long to execute
 * on the main thread, which can cause Application Not Responding (ANR) errors.
 *
 * Key features:
 * - Logs warnings for operations taking > 100ms
 * - Logs errors for operations taking > 1000ms
 * - Tracks execution statistics
 * - Provides visibility into main thread bottlenecks
 *
 * Usage:
 * ```
 * val safeHandler = SafeMainThreadHandler("MyComponent")
 * safeHandler.post {
 *   // UI operation
 * }
 * ```
 */
class SafeMainThreadHandler(private val componentName: String = "Unknown") {

    companion object {
        private const val TAG = "SafeMainThreadHandler"
        private const val WARNING_THRESHOLD_MS = 100L
        private const val ERROR_THRESHOLD_MS = 1000L

        private val totalOperations = AtomicLong(0)
        private val slowOperations = AtomicLong(0)
        private val verySlowOperations = AtomicLong(0)

        fun getStatistics(): HandlerStatistics {
            return HandlerStatistics(
                totalOperations = totalOperations.get(),
                slowOperations = slowOperations.get(),
                verySlowOperations = verySlowOperations.get()
            )
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Post a runnable to the main thread with execution time monitoring
     */
    fun post(runnable: Runnable) {
        handler.post(MonitoredRunnable(runnable, componentName))
    }

    /**
     * Post a delayed runnable to the main thread with execution time monitoring
     */
    fun postDelayed(runnable: Runnable, delayMillis: Long) {
        handler.postDelayed(MonitoredRunnable(runnable, componentName), delayMillis)
    }

    /**
     * Remove all callbacks and messages
     */
    fun removeCallbacksAndMessages() {
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Wrapper that monitors execution time of runnables
     */
    private class MonitoredRunnable(
        private val wrapped: Runnable,
        private val componentName: String
    ) : Runnable {
        override fun run() {
            val startTime = System.nanoTime()
            totalOperations.incrementAndGet()

            try {
                wrapped.run()
            } finally {
                val executionTime = (System.nanoTime() - startTime) / 1_000_000

                when {
                    executionTime > ERROR_THRESHOLD_MS -> {
                        verySlowOperations.incrementAndGet()
                        Log.e(
                            TAG,
                            "[$componentName] CRITICAL: Main thread blocked for ${executionTime}ms! " +
                                    "This may cause ANR. Move work to background thread."
                        )
                    }

                    executionTime > WARNING_THRESHOLD_MS -> {
                        slowOperations.incrementAndGet()
                        Log.w(
                            TAG,
                            "[$componentName] WARNING: Main thread operation took ${executionTime}ms. " +
                                    "Consider optimizing or moving to background thread."
                        )
                    }
                }
            }
        }
    }

    data class HandlerStatistics(
        val totalOperations: Long,
        val slowOperations: Long,
        val verySlowOperations: Long
    ) {
        val slowOperationRate: Float
            get() = if (totalOperations > 0) {
                (slowOperations.toFloat() / totalOperations.toFloat()) * 100f
            } else 0f

        val criticalOperationRate: Float
            get() = if (totalOperations > 0) {
                (verySlowOperations.toFloat() / totalOperations.toFloat()) * 100f
            } else 0f
    }
}
