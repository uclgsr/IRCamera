package mpdc4gsr.core.monitoring

import android.content.Context
import android.os.Build
import mpdc4gsr.core.utils.AppLogger
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Telemetry and observability manager for IRCamera.
 *
 * Tracks app health, user behavior, and system metrics for:
 * - Crash/ANR monitoring
 * - Feature usage analytics
 * - Performance tracking
 * - Error rate monitoring
 *
 * Integration points:
 * - Firebase Crashlytics (crash reporting)
 * - Firebase Analytics (user behavior)
 * - Custom backend (custom metrics)
 * - Local logging (development)
 */
object TelemetryManager {

    private const val TAG = "TelemetryManager"

    private var isInitialized = false
    private var userId: String? = null
    private var sessionId: String? = null
    private val properties = ConcurrentHashMap<String, String>()

    /**
     * Initialize telemetry system.
     * Call this early in Application.onCreate().
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            AppLogger.w(TAG, "TelemetryManager already initialized")
            return
        }

        try {
            sessionId = generateSessionId()

            setDeviceProperties(context)

            isInitialized = true
            AppLogger.i(TAG, "TelemetryManager initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize TelemetryManager", e)
        }
    }

    /**
     * Set user identifier for tracking.
     */
    fun setUserId(id: String) {
        userId = id
        AppLogger.d(TAG, "User ID set")
    }

    /**
     * Clear user identifier (on logout).
     */
    fun clearUserId() {
        userId = null
        AppLogger.d(TAG, "User ID cleared")
    }

    /**
     * Track an event.
     *
     * @param eventName Name of the event
     * @param params Optional parameters
     */
    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {
        if (!isInitialized) {
            AppLogger.w(TAG, "TelemetryManager not initialized")
            return
        }

        try {
            val eventData = buildEventData(eventName, params)
            AppLogger.i(TAG, "Event tracked: $eventName")

            // TODO: Send to analytics backend
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to track event: $eventName", e)
        }
    }

    /**
     * Track screen view.
     *
     * @param screenName Name of the screen
     * @param screenClass Class name of the screen
     */
    fun trackScreenView(screenName: String, screenClass: String) {
        trackEvent(
            "screen_view", mapOf(
                "screen_name" to screenName,
                "screen_class" to screenClass
            )
        )
    }

    /**
     * Track an error.
     *
     * @param error Error message or description
     * @param exception Optional exception
     * @param fatal Whether the error is fatal
     */
    fun trackError(error: String, exception: Throwable? = null, fatal: Boolean = false) {
        try {
            AppLogger.e(TAG, "Error tracked: $error", exception)

            val errorData = mapOf(
                "error" to error,
                "fatal" to fatal,
                "exception_type" to (exception?.javaClass?.simpleName ?: "Unknown"),
                "stack_trace" to (exception?.stackTraceToString() ?: "")
            )

            trackEvent(if (fatal) "fatal_error" else "error", errorData)

            // TODO: Send to crash reporting service
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to track error", e)
        }
    }

    /**
     * Log a custom metric.
     *
     * @param metricName Name of the metric
     * @param value Metric value
     * @param unit Optional unit
     */
    fun logMetric(metricName: String, value: Number, unit: String? = null) {
        try {
            AppLogger.i(TAG, "Metric: $metricName = $value${unit?.let { " $it" } ?: ""}")

            val metricData = mutableMapOf<String, Any>(
                "metric_name" to metricName,
                "value" to value
            )
            unit?.let { metricData["unit"] = it }

            trackEvent("metric_logged", metricData)

            // TODO: Send to metrics backend
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to log metric: $metricName", e)
        }
    }

    /**
     * Track recording session.
     *
     * @param recordingId Unique recording identifier
     * @param durationMs Duration in milliseconds
     * @param success Whether recording completed successfully
     */
    fun trackRecordingSession(recordingId: String, durationMs: Long, success: Boolean) {
        trackEvent(
            "recording_session", mapOf(
                "recording_id" to recordingId,
                "duration_ms" to durationMs,
                "duration_seconds" to (durationMs / 1000),
                "success" to success
            )
        )
    }

    /**
     * Track feature usage.
     *
     * @param featureName Name of the feature
     * @param action Action performed
     */
    fun trackFeatureUsage(featureName: String, action: String) {
        trackEvent(
            "feature_usage", mapOf(
                "feature" to featureName,
                "action" to action
            )
        )
    }

    /**
     * Track network request.
     *
     * @param endpoint API endpoint
     * @param method HTTP method
     * @param statusCode Response status code
     * @param durationMs Request duration in milliseconds
     */
    fun trackNetworkRequest(
        endpoint: String,
        method: String,
        statusCode: Int,
        durationMs: Long
    ) {
        trackEvent(
            "network_request", mapOf(
                "endpoint" to endpoint,
                "method" to method,
                "status_code" to statusCode,
                "duration_ms" to durationMs,
                "success" to (statusCode in 200..299)
            )
        )
    }

    /**
     * Track permission request result.
     *
     * @param permission Permission name
     * @param granted Whether permission was granted
     */
    fun trackPermissionRequest(permission: String, granted: Boolean) {
        trackEvent(
            "permission_request", mapOf(
                "permission" to permission,
                "granted" to granted
            )
        )
    }

    /**
     * Set a custom property that will be included in all events.
     *
     * @param key Property key
     * @param value Property value
     */
    fun setProperty(key: String, value: String) {
        properties[key] = value
    }

    /**
     * Remove a custom property.
     *
     * @param key Property key
     */
    fun removeProperty(key: String) {
        properties.remove(key)
    }

    /**
     * Build event data with common properties.
     */
    private fun buildEventData(eventName: String, params: Map<String, Any>?): JSONObject {
        return JSONObject().apply {
            put("event_name", eventName)
            put("timestamp", System.currentTimeMillis())
            put("session_id", sessionId)
            userId?.let { put("user_id", it) }

            properties.forEach { (key, value) ->
                put(key, value)
            }

            params?.forEach { (key, value) ->
                put(key, value)
            }
        }
    }

    /**
     * Set device properties.
     */
    private fun setDeviceProperties(context: Context) {
        setProperty("device_model", Build.MODEL)
        setProperty("device_manufacturer", Build.MANUFACTURER)
        setProperty("android_version", Build.VERSION.RELEASE)
        setProperty("sdk_version", Build.VERSION.SDK_INT.toString())
        setProperty("app_version", getAppVersion(context))
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
    }
}

/**
 * Extension functions for easy telemetry tracking.
 */

/**
 * Track execution time of a block.
 *
 * Usage:
 * ```
 * trackExecutionTime("database_query") {
 *     database.query(...)
 * }
 * ```
 */
inline fun <T> trackExecutionTime(operationName: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    try {
        return block()
    } finally {
        val duration = System.currentTimeMillis() - startTime
        TelemetryManager.logMetric("${operationName}_duration", duration, "ms")
    }
}

/**
 * Suspend version of trackExecutionTime.
 */
suspend inline fun <T> trackExecutionTimeSuspend(
    operationName: String,
    crossinline block: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
    try {
        return block()
    } finally {
        val duration = System.currentTimeMillis() - startTime
        TelemetryManager.logMetric("${operationName}_duration", duration, "ms")
    }
}
