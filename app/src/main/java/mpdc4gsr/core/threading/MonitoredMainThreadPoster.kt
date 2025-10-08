package mpdc4gsr.core.threading

import android.os.Handler
import android.os.Looper

object MonitoredMainThreadPoster {
    private const val WARNING_THRESHOLD_MS = 100L
    private const val CRITICAL_THRESHOLD_MS = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val totalPosts = java.util.concurrent.atomic.AtomicLong(0L)
    private val slowPosts = java.util.concurrent.atomic.AtomicLong(0L)
    private val criticalPosts = java.util.concurrent.atomic.AtomicLong(0L)
    fun post(componentName: String, runnable: Runnable) {
        handler.post(MonitoredRunnable(componentName, runnable))
    }

    fun post(componentName: String, action: () -> Unit) {
        handler.post(MonitoredRunnable(componentName, Runnable(action)))
    }

    fun postDelayed(componentName: String, delayMillis: Long, runnable: Runnable) {
        handler.postDelayed(MonitoredRunnable(componentName, runnable), delayMillis)
    }

    fun postDelayed(componentName: String, delayMillis: Long, action: () -> Unit) {
        handler.postDelayed(MonitoredRunnable(componentName, Runnable(action)), delayMillis)
    }

    fun removeCallbacksAndMessages() {
        handler.removeCallbacksAndMessages(null)
    }

    fun getStatistics(): PostStatistics {
        return PostStatistics(
            totalPosts = totalPosts.get(),
            slowPosts = slowPosts.get(),
            criticalPosts = criticalPosts.get()
        )
    }

    fun resetStatistics() {
        totalPosts.set(0)
        slowPosts.set(0)
        criticalPosts.set(0)
    }

    private class MonitoredRunnable(
        private val componentName: String,
        private val wrapped: Runnable
    ) : Runnable {
        override fun run() {
            val startTime = System.nanoTime()
            totalPosts.incrementAndGet()
                wrapped.run()
                val executionTime = (System.nanoTime() - startTime) / 1_000_000
                when {
                    executionTime > CRITICAL_THRESHOLD_MS -> {
                        criticalPosts.incrementAndGet()
                            TAG,
                            "[$componentName] CRITICAL ANR RISK: Main thread blocked for ${executionTime}ms! " +
                                    "This WILL cause ANR. Move work to background thread immediately."
                        )
                    }

                    executionTime > WARNING_THRESHOLD_MS -> {
                        slowPosts.incrementAndGet()
                            TAG,
                            "[$componentName] WARNING: Main thread operation took ${executionTime}ms. " +
                                    "Consider optimizing or moving to background thread to prevent ANR."
                        )
                    }
                }
            }
        }
    }

    data class PostStatistics(
        val totalPosts: Long,
        val slowPosts: Long,
        val criticalPosts: Long
    ) {
        val slowPostRate: Float
            get() = if (totalPosts > 0) {
                (slowPosts.toFloat() / totalPosts.toFloat()) * 100f
            } else 0f
        val criticalPostRate: Float
            get() = if (totalPosts > 0) {
                (criticalPosts.toFloat() / totalPosts.toFloat()) * 100f
            } else 0f

        fun hasAnrRisk(): Boolean = criticalPosts > 0
        fun needsOptimization(): Boolean = slowPostRate > 5f
    }
}
