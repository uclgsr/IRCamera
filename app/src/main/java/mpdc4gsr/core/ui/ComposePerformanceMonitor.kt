package mpdc4gsr.core.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mpdc4gsr.core.utils.AppLogger
import kotlin.system.measureTimeMillis

object ComposePerformanceMonitor {
    private const val TAG = "ComposePerformance"

    // Performance thresholds
    private const val FRAME_BUDGET_MS = 16L // 60fps target
    const val MAX_SAMPLES = 100
    private val _recompositionCount = MutableStateFlow(0)
    val recompositionCount: StateFlow<Int> = _recompositionCount
    private val _frameTimeMs = MutableStateFlow(0L)
    val frameTimeMs: StateFlow<Long> = _frameTimeMs
    private val _memoryUsageMb = MutableStateFlow(0f)
    val memoryUsageMb: StateFlow<Float> = _memoryUsageMb
    private val _navigationLatencyMs = MutableStateFlow(0L)
    val navigationLatencyMs: StateFlow<Long> = _navigationLatencyMs
    private val performanceMetrics = mutableMapOf<String, PerformanceMetric>()

    @Composable
    fun TrackRecomposition(name: String, content: @Composable () -> Unit) {
        val recompositionCount = remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            recompositionCount.intValue++
            _recompositionCount.value = _recompositionCount.value + 1
            AppLogger.d(TAG, "$name recomposed ${recompositionCount.intValue} times")
        }
        content()
    }

    @Composable
    fun <T> MeasureCompositionTime(
        name: String,
        content: @Composable () -> T
    ): T {
        val startTime = remember { System.currentTimeMillis() }
        val result = content()
        LaunchedEffect(result) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            _frameTimeMs.value = duration
            recordMetric(name, duration)
            AppLogger.d(TAG, "$name composition took ${duration}ms")
        }
        return result
    }

    fun trackMemoryUsage(operationName: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        _memoryUsageMb.value = usedMemory.toFloat()
        AppLogger.d(TAG, "$operationName memory usage: ${usedMemory}MB")
    }

    fun trackNavigation(route: String, startTime: Long) {
        val latency = System.currentTimeMillis() - startTime
        _navigationLatencyMs.value = latency
        recordMetric("navigation_$route", latency)
        AppLogger.d(TAG, "Navigation to $route took ${latency}ms")
    }

    private fun recordMetric(name: String, value: Long) {
        val metric = performanceMetrics.getOrPut(name) { PerformanceMetric(name) }
        metric.addSample(value)
    }

    fun getPerformanceSummary(): Map<String, PerformanceMetric> {
        return performanceMetrics.toMap()
    }

    fun Modifier.trackDrawPerformance(name: String): Modifier = this.drawWithContent {
        val drawTime = measureTimeMillis {
            drawContent()
        }
        if (drawTime > FRAME_BUDGET_MS) {
            AppLogger.w(TAG, "$name draw took ${drawTime}ms (potential jank)")
        }
    }
}

data class PerformanceMetric(
    val name: String,
    private val samples: MutableList<Long> = mutableListOf()
) {
    fun addSample(value: Long) {
        samples.add(value)
        // Keep only last MAX_SAMPLES samples to prevent memory growth
        if (samples.size > ComposePerformanceMonitor.MAX_SAMPLES) {
            samples.removeAt(0)
        }
    }

    val average: Double get() = if (samples.isEmpty()) 0.0 else samples.average()
    val max: Long get() = samples.maxOrNull() ?: 0L
    val min: Long get() = samples.minOrNull() ?: 0L
    val count: Int get() = samples.size
}

@Composable
fun PerformanceOverlay(
    showOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!showOverlay) return
    val recompositionCount by ComposePerformanceMonitor.recompositionCount.collectAsState()
    val frameTime by ComposePerformanceMonitor.frameTimeMs.collectAsState()
    val memoryUsage by ComposePerformanceMonitor.memoryUsageMb.collectAsState()
    val navigationLatency by ComposePerformanceMonitor.navigationLatencyMs.collectAsState()
    val density = LocalDensity.current
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            // Draw performance overlay
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = Color.White.toArgb()
                    textSize = with(density) { 12.dp.toPx() }
                    isAntiAlias = true
                }
                val backgroundPaint = android.graphics.Paint().apply {
                    color = Color.Black.copy(alpha = 0.6f).toArgb()
                }
                val metrics = listOf(
                    "Recompositions: $recompositionCount",
                    "Frame Time: ${frameTime}ms",
                    "Memory: ${String.format("%.1f", memoryUsage)}MB",
                    "Nav Latency: ${navigationLatency}ms"
                )
                val textHeight = 40f
                val overlayHeight = metrics.size * textHeight + 20f
                // Draw background
                canvas.nativeCanvas.drawRect(
                    10f,
                    10f,
                    300f,
                    overlayHeight,
                    backgroundPaint
                )
                // Draw metrics
                metrics.forEachIndexed { index, metric ->
                    canvas.nativeCanvas.drawText(
                        metric,
                        20f,
                        40f + (index * textHeight),
                        paint
                    )
                }
            }
        }
    )
}

object SensorDataPerformanceTracker {
    private const val SENSOR_PROCESSING_THRESHOLD_MS = 100L
    private const val SENSOR_PROCESSING_CRITICAL_MS = 200L
    private const val NAVIGATION_SLOW_THRESHOLD_MS = 300L
    fun trackGSRDataProcessing(dataPoints: Int, processingTimeMs: Long) {
        val throughput = dataPoints / (processingTimeMs / 1000.0)
        Log.d(
            "SensorPerformance",
            "GSR processing: $dataPoints points in ${processingTimeMs}ms (${
                String.format(
                    "%.1f",
                    throughput
                )
            } points/sec)"
        )
        if (processingTimeMs > SENSOR_PROCESSING_THRESHOLD_MS) {
            Log.w(
                "SensorPerformance",
                "GSR processing slower than expected: ${processingTimeMs}ms for $dataPoints points"
            )
        }
    }

    fun trackThermalImageProcessing(imageSize: String, processingTimeMs: Long) {
        AppLogger.d("SensorPerformance", "Thermal image processing: $imageSize in ${processingTimeMs}ms")
        if (processingTimeMs > SENSOR_PROCESSING_CRITICAL_MS) {
            Log.w(
                "SensorPerformance",
                "Thermal processing slower than expected: ${processingTimeMs}ms"
            )
        }
    }

    fun trackNavigationPerformance(fromRoute: String, toRoute: String, transitionTimeMs: Long) {
        AppLogger.d("SensorPerformance", "Navigation from $fromRoute to $toRoute: ${transitionTimeMs}ms")
        if (transitionTimeMs > NAVIGATION_SLOW_THRESHOLD_MS) {
            AppLogger.w("SensorPerformance", "Navigation slower than expected: ${transitionTimeMs}ms")
        }
    }
}

object ComposeMemoryOptimizer {
    // Memory pressure thresholds
    private const val MEMORY_CRITICAL_THRESHOLD = 0.9f
    private const val MEMORY_HIGH_THRESHOLD = 0.7f
    private const val MEMORY_MODERATE_THRESHOLD = 0.5f

    fun checkMemoryPressure(): MemoryPressureLevel {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryRatio = usedMemory.toFloat() / maxMemory.toFloat()
        return when {
            memoryRatio > MEMORY_CRITICAL_THRESHOLD -> MemoryPressureLevel.CRITICAL
            memoryRatio > MEMORY_HIGH_THRESHOLD -> MemoryPressureLevel.HIGH
            memoryRatio > MEMORY_MODERATE_THRESHOLD -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.LOW
        }
    }

    fun getOptimizationSuggestions(pressureLevel: MemoryPressureLevel): List<String> {
        return when (pressureLevel) {
            MemoryPressureLevel.CRITICAL -> listOf(
                "Consider reducing image resolution",
                "Implement lazy loading for large datasets",
                "Clear unused caches",
                "Reduce number of concurrent operations"
            )

            MemoryPressureLevel.HIGH -> listOf(
                "Optimize image loading",
                "Use lighter data structures",
                "Consider pagination for long lists"
            )

            MemoryPressureLevel.MODERATE -> listOf(
                "Monitor memory usage trends",
                "Consider preemptive cleanup"
            )

            MemoryPressureLevel.LOW -> listOf(
                "Memory usage is optimal"
            )
        }
    }
}

enum class MemoryPressureLevel {
    LOW, MODERATE, HIGH, CRITICAL
}