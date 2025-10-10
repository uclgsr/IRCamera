package mpdc4gsr.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import mpdc4gsr.core.ui.ComposePerformanceMonitor

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
            }
        }
    }

    fun logPerformanceSummary() {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        if (navigationMetrics.isEmpty()) {
            return
        }
        navigationMetrics.forEach { (route, metric) ->
            val routeName = route.removePrefix("navigation_")
        }
        val slowRoutes = navigationMetrics.filter { it.value.average > WARNING_THRESHOLD_MS }
        if (slowRoutes.isNotEmpty()) {
            slowRoutes.forEach { (route, metric) ->
                val routeName = route.removePrefix("navigation_")
            }
        }
    }

    fun getSlowRoutes(thresholdMs: Long = WARNING_THRESHOLD_MS): List<Pair<String, Double>> {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .filter { it.value.average > thresholdMs }
            .map {
                it.key.removePrefix("navigation_") to it.value.average
            }.sortedByDescending { it.second }
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
