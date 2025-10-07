package mpdc4gsr.core.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import mpdc4gsr.core.ui.ComposePerformanceMonitor
import mpdc4gsr.core.utils.AppLogger

object NavigationPerformanceHelper {
    private const val TAG = "NavigationPerf"
    private const val WARNING_THRESHOLD_MS = 300L

    @Composable
    fun TrackNavigation(routeName: String) {
        val startTime = remember { System.currentTimeMillis() }
        LaunchedEffect(Unit) {
            val latency = System.currentTimeMillis() - startTime
            ComposePerformanceMonitor.trackNavigation(routeName, startTime)
            if (latency > WARNING_THRESHOLD_MS) {
                AppLogger.w(TAG, "Slow navigation to $routeName: ${latency}ms (threshold: ${WARNING_THRESHOLD_MS}ms)")
            }
        }
    }

    fun logPerformanceSummary() {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        AppLogger.d(TAG, "=== Navigation Performance Summary ===")
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        if (navigationMetrics.isEmpty()) {
            AppLogger.d(TAG, "No navigation metrics recorded yet")
            return
        }
        navigationMetrics.forEach { (route, metric) ->
            val routeName = route.removePrefix("navigation_")
            Log.d(
                TAG, "$routeName: avg=${String.format("%.1f", metric.average)}ms, " +
                        "max=${metric.max}ms, min=${metric.min}ms, count=${metric.count}"
            )
        }
        val slowRoutes = navigationMetrics.filter { it.value.average > WARNING_THRESHOLD_MS }
        if (slowRoutes.isNotEmpty()) {
            AppLogger.w(TAG, "=== Routes Exceeding Threshold (${WARNING_THRESHOLD_MS}ms) ===")
            slowRoutes.forEach { (route, metric) ->
                val routeName = route.removePrefix("navigation_")
                AppLogger.w(TAG, "$routeName: avg=${String.format("%.1f", metric.average)}ms")
            }
        }
        AppLogger.d(TAG, "======================================")
    }

    fun getSlowRoutes(thresholdMs: Long = WARNING_THRESHOLD_MS): List<Pair<String, Double>> {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .filter { it.value.average > thresholdMs }
            .map {
                it.key.removePrefix("navigation_") to it.value.average
            }
            .sortedByDescending { it.second }
    }

    fun getFastestRoute(): Pair<String, Double>? {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .minByOrNull { it.value.average }
            ?.let { it.key.removePrefix("navigation_") to it.value.average }
    }

    fun getSlowestRoute(): Pair<String, Double>? {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .maxByOrNull { it.value.average }
            ?.let { it.key.removePrefix("navigation_") to it.value.average }
    }
}
