package mpdc4gsr.core.monitoring

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object TelemetryManager {
    private var isInitialized = false
    private var userId: String? = null
    private var sessionId: String? = null
    private val properties = ConcurrentHashMap<String, String>()

    fun initialize(context: Context) {
        if (isInitialized) {
            return
        }
            sessionId = generateSessionId()
            setDeviceProperties(context)
            isInitialized = true
        }
    }

    fun setUserId(id: String) {
        userId = id
    }

    fun clearUserId() {
        userId = null
    }

    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {
        if (!isInitialized) {
            return
        }
            val eventData = buildEventData(eventName, params)
            // TODO: Send to analytics backend
        }
    }

    fun trackScreenView(screenName: String, screenClass: String) {
        trackEvent(
            "screen_view", mapOf(
                "screen_name" to screenName,
                "screen_class" to screenClass
            )
        )
    }

    fun trackError(error: String, exception: Throwable? = null, fatal: Boolean = false) {
            val errorData = mapOf(
                "error" to error,
                "fatal" to fatal,
                "exception_type" to (exception?.javaClass?.simpleName ?: "Unknown"),
                "stack_trace" to (exception?.stackTraceToString() ?: "")
            )
            trackEvent(if (fatal) "fatal_error" else "error", errorData)
            // TODO: Send to crash reporting service
        }
    }

    fun logMetric(metricName: String, value: Number, unit: String? = null) {
            val metricData = mutableMapOf<String, Any>(
                "metric_name" to metricName,
                "value" to value
            )
            unit?.let { metricData["unit"] = it }
            trackEvent("metric_logged", metricData)
            // TODO: Send to metrics backend
        }
    }

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

    fun trackFeatureUsage(featureName: String, action: String) {
        trackEvent(
            "feature_usage", mapOf(
                "feature" to featureName,
                "action" to action
            )
        )
    }

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

    fun trackPermissionRequest(permission: String, granted: Boolean) {
        trackEvent(
            "permission_request", mapOf(
                "permission" to permission,
                "granted" to granted
            )
        )
    }

    fun setProperty(key: String, value: String) {
        properties[key] = value
    }

    fun removeProperty(key: String) {
        properties.remove(key)
    }

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

    private fun setDeviceProperties(context: Context) {
        setProperty("device_model", Build.MODEL)
        setProperty("device_manufacturer", Build.MANUFACTURER)
        setProperty("android_version", Build.VERSION.RELEASE)
        setProperty("sdk_version", Build.VERSION.SDK_INT.toString())
        setProperty("app_version", getAppVersion(context))
    }

    private fun getAppVersion(context: Context): String {
        return (
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
            "Unknown"
        }
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
    }
}


inline fun <T> trackExecutionTime(operationName: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
        return block()
        val duration = System.currentTimeMillis() - startTime
        TelemetryManager.logMetric("${operationName}_duration", duration, "ms")
    }
}

suspend inline fun <T> trackExecutionTimeSuspend(
    operationName: String,
    crossinline block: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
        return block()
        val duration = System.currentTimeMillis() - startTime
        TelemetryManager.logMetric("${operationName}_duration", duration, "ms")
    }
}
