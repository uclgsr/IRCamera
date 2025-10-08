package mpdc4gsr.core.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import mpdc4gsr.core.StructuredLogger
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class SecurityMonitor(
    private val context: Context,
    private val logger: StructuredLogger,
) {
    companion object {
        private const val TAG = "SecurityMonitor"
        private const val MAX_FAILED_LOGINS_PER_HOUR = 10
        private const val MAX_CONNECTIONS_PER_DEVICE = 5
        private const val SUSPICIOUS_ACTIVITY_THRESHOLD = 5
        private const val SESSION_TIMEOUT_WARNING_MS = 5 * 60 * 1000L
        private const val MONITORING_INTERVAL_MS = 30 * 1000L
        private const val CLEANUP_INTERVAL_MS = 60 * 60 * 1000L
        const val ALERT_BRUTE_FORCE = "brute_force_attack"
        const val ALERT_SUSPICIOUS_CONNECTION = "suspicious_connection"
        const val ALERT_UNUSUAL_ACTIVITY = "unusual_activity"
        const val ALERT_SESSION_HIJACK = "session_hijack_attempt"
        const val ALERT_CERTIFICATE_VIOLATION = "certificate_violation"
        const val ALERT_PERMISSION_ESCALATION = "permission_escalation"
        const val ALERT_DATA_EXFILTRATION = "data_exfiltration"
        const val ALERT_SYSTEM_COMPROMISE = "system_compromise"
    }

    private val isMonitoring = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val failedLogins = ConcurrentHashMap<String, MutableList<Long>>()
    private val sessionActivities = ConcurrentHashMap<String, SessionActivity>()
    private val securityAlerts = mutableListOf<SecurityAlert>()
    private val totalConnections = AtomicLong(0)
    private val totalFailedLogins = AtomicLong(0)
    private val totalSecurityAlerts = AtomicLong(0)

    data class SessionActivity(
        val deviceId: String,
        val startTime: Long,
        var lastActivity: Long,
        var activityCount: Long,
        var suspiciousEvents: Int,
        val activityPattern: MutableList<ActivityEvent>,
    )

    data class ActivityEvent(
        val type: String,
        val timestamp: Long,
        val details: Map<String, Any>,
    )

    data class SecurityAlert(
        val id: String,
        val type: String,
        val severity: Severity,
        val deviceId: String,
        val timestamp: Long,
        val description: String,
        val details: Map<String, Any>,
        var acknowledged: Boolean = false,
    )

    enum class Severity(val level: Int, val displayName: String) {
        LOW(1, "Low"),
        MEDIUM(2, "Medium"),
        HIGH(3, "High"),
        CRITICAL(4, "Critical"),
    }

    interface SecurityEventListener {
        fun onSecurityAlert(alert: SecurityAlert)
        fun onSuspiciousActivity(
            deviceId: String,
            activityType: String,
            details: Map<String, Any>,
        )

        fun onSessionAnomalyDetected(
            deviceId: String,
            anomalyType: String,
        )

        fun onThreatDetected(
            threatType: String,
            confidence: Float,
            details: Map<String, Any>,
        )
    }

    private var securityListener: SecurityEventListener? = null
    fun initialize(): Boolean {
        return try {
            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "security_monitor_initialized",
                mapOf(
                    "monitoring_interval_seconds" to (MONITORING_INTERVAL_MS / 1000L),
                    "cleanup_interval_minutes" to (CLEANUP_INTERVAL_MS / (60 * 1000L)),
                    "alert_types_count" to 8,
                ),
            )
            true
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "init_failed",
                mapOf(
                    "error" to e.message.orEmpty(),
                ),
            )
            false
        }
    }

    fun setSecurityEventListener(listener: SecurityEventListener) {
        this.securityListener = listener
    }

    fun startMonitoring() {
        if (isMonitoring.get()) {
            return
        }
        isMonitoring.set(true)
        scope.launch {
            while (isMonitoring.get()) {
                try {
                    performSecurityCheck()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                }
            }
        }
        scope.launch {
            while (isMonitoring.get()) {
                try {
                    performCleanup()
                    delay(CLEANUP_INTERVAL_MS)
                } catch (e: Exception) {
                }
            }
        }
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "monitoring_started",
            mapOf(
                "monitoring_active" to true,
            ),
        )
    }

    fun stopMonitoring() {
        isMonitoring.set(false)
        scope.cancel()
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "monitoring_stopped",
            mapOf(
                "total_connections_monitored" to totalConnections.get(),
                "total_failed_logins" to totalFailedLogins.get(),
                "total_alerts_generated" to totalSecurityAlerts.get(),
            ),
        )
    }

    fun reportConnectionAttempt(
        deviceId: String,
        successful: Boolean,
        details: Map<String, Any> = emptyMap(),
    ) {
        val currentTime = System.currentTimeMillis()
        connectionAttempts.computeIfAbsent(deviceId) { mutableListOf() }.add(currentTime)
        totalConnections.incrementAndGet()
        if (!successful) {
            failedLogins.computeIfAbsent(deviceId) { mutableListOf() }.add(currentTime)
            totalFailedLogins.incrementAndGet()
            checkBruteForceAttack(deviceId)
        }
        updateSessionActivity(
            deviceId,
            "connection_attempt",
            details + mapOf("successful" to successful)
        )
        logger.log(
            StructuredLogger.LogLevel.DEBUG,
            TAG,
            "connection_attempt",
            mapOf(
                "device_id" to deviceId,
                "successful" to successful,
                "timestamp" to currentTime,
            ),
        )
    }

    fun reportSecurityEvent(
        eventType: String,
        details: Map<String, Any>,
    ) {
        val deviceId = details["device_id"] as? String ?: "unknown"
        updateSessionActivity(deviceId, eventType, details)
        val severity = determineSeverity(eventType, details)
        if (severity.level >= Severity.MEDIUM.level) {
            generateSecurityAlert(eventType, severity, deviceId, details)
        }
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "security_event",
            mapOf(
                "event_type" to eventType,
                "device_id" to deviceId,
                "severity" to severity.name,
            ),
        )
    }

    fun checkSessionActivity(deviceId: String) {
        val activity = sessionActivities[deviceId] ?: return
        val currentTime = System.currentTimeMillis()
        if (currentTime - activity.lastActivity > SESSION_TIMEOUT_WARNING_MS) {
            securityListener?.onSessionAnomalyDetected(deviceId, "session_timeout_warning")
        }
        if (activity.activityCount > 100 && (currentTime - activity.startTime) < 60 * 1000L) {
            generateSecurityAlert(
                ALERT_UNUSUAL_ACTIVITY,
                Severity.MEDIUM,
                deviceId,
                mapOf(
                    "activity_count" to activity.activityCount,
                    "time_window_seconds" to ((currentTime - activity.startTime) / 1000L),
                ),
            )
        }
        if (activity.suspiciousEvents >= SUSPICIOUS_ACTIVITY_THRESHOLD) {
            generateSecurityAlert(
                ALERT_SUSPICIOUS_CONNECTION,
                Severity.HIGH,
                deviceId,
                mapOf(
                    "suspicious_events_count" to activity.suspiciousEvents,
                    "session_duration_minutes" to ((currentTime - activity.startTime) / (60 * 1000L)),
                ),
            )
        }
    }

    private suspend fun performSecurityCheck() {
        val currentTime = System.currentTimeMillis()
        sessionActivities.values.forEach { activity ->
            checkSessionActivity(activity.deviceId)
        }
        checkConnectionPatterns()
        checkCertificateViolations()
        analyzeThreatPatterns()
        updateMonitoringStatistics()
    }

    private fun checkBruteForceAttack(deviceId: String) {
        val recentFailures = getRecentFailedLogins(deviceId, 60 * 60 * 1000L)
        if (recentFailures.size >= MAX_FAILED_LOGINS_PER_HOUR) {
            generateSecurityAlert(
                ALERT_BRUTE_FORCE,
                Severity.HIGH,
                deviceId,
                mapOf(
                    "failed_attempts" to recentFailures.size,
                    "time_window" to "1_hour",
                ),
            )
        }
    }

    private fun checkConnectionPatterns() {
        connectionAttempts.forEach { (deviceId, attempts) ->
            val recentAttempts =
                attempts.filter {
                    System.currentTimeMillis() - it < 60 * 1000L
                }
            if (recentAttempts.size > MAX_CONNECTIONS_PER_DEVICE) {
                generateSecurityAlert(
                    ALERT_SUSPICIOUS_CONNECTION,
                    Severity.MEDIUM,
                    deviceId,
                    mapOf(
                        "connections_per_minute" to recentAttempts.size,
                        "threshold" to MAX_CONNECTIONS_PER_DEVICE,
                    ),
                )
            }
        }
    }

    private fun checkCertificateViolations() {
    }

    private fun analyzeThreatPatterns() {
        val recentAlerts = getRecentAlerts(60 * 60 * 1000L)
        val alertsByDevice = recentAlerts.groupBy { it.deviceId }
        alertsByDevice.forEach { (deviceId, alerts) ->
            if (alerts.size >= 5) {
                securityListener?.onThreatDetected(
                    "device_compromise",
                    0.8f,
                    mapOf(
                        "device_id" to deviceId,
                        "alert_count" to alerts.size,
                        "alert_types" to alerts.map { it.type }.distinct(),
                    ),
                )
            }
        }
        if (recentAlerts.size >= 10) {
            val uniqueDevices = recentAlerts.map { it.deviceId }.distinct().size
            if (uniqueDevices >= 3) {
                securityListener?.onThreatDetected(
                    "coordinated_attack",
                    0.9f,
                    mapOf(
                        "affected_devices" to uniqueDevices,
                        "total_alerts" to recentAlerts.size,
                    ),
                )
            }
        }
    }

    private fun updateSessionActivity(
        deviceId: String,
        activityType: String,
        details: Map<String, Any>,
    ) {
        val currentTime = System.currentTimeMillis()
        val activity =
            sessionActivities.computeIfAbsent(deviceId) {
                SessionActivity(
                    deviceId = deviceId,
                    startTime = currentTime,
                    lastActivity = currentTime,
                    activityCount = 0,
                    suspiciousEvents = 0,
                    activityPattern = mutableListOf(),
                )
            }
        activity.lastActivity = currentTime
        activity.activityCount++
        if (isSuspiciousActivity(activityType, details)) {
            activity.suspiciousEvents++
        }
        activity.activityPattern.add(ActivityEvent(activityType, currentTime, details))
        if (activity.activityPattern.size > 1000) {
            activity.activityPattern.removeAt(0)
        }
    }

    private fun isSuspiciousActivity(
        activityType: String,
        details: Map<String, Any>,
    ): Boolean {
        return when (activityType) {
            "connection_attempt" -> !(details["successful"] as? Boolean ?: true)
            "permission_denied" -> true
            "certificate_invalid" -> true
            "session_hijack_attempt" -> true
            "unusual_data_access" -> true
            else -> false
        }
    }

    private fun generateSecurityAlert(
        alertType: String,
        severity: Severity,
        deviceId: String,
        details: Map<String, Any>,
    ) {
        val alert =
            SecurityAlert(
                id = generateAlertId(),
                type = alertType,
                severity = severity,
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                description = generateAlertDescription(alertType, details),
                details = details,
            )
        synchronized(securityAlerts) {
            securityAlerts.add(alert)
            if (securityAlerts.size > 1000) {
                securityAlerts.removeAt(0)
            }
        }
        totalSecurityAlerts.incrementAndGet()
        securityListener?.onSecurityAlert(alert)
        logger.log(
            StructuredLogger.LogLevel.WARNING,
            TAG,
            "security_alert",
            mapOf(
                "alert_id" to alert.id,
                "alert_type" to alertType,
                "severity" to severity.name,
                "device_id" to deviceId,
            ),
        )
    }

    private fun determineSeverity(
        eventType: String,
        details: Map<String, Any>,
    ): Severity {
        return when (eventType) {
            ALERT_BRUTE_FORCE -> Severity.HIGH
            ALERT_SESSION_HIJACK -> Severity.CRITICAL
            ALERT_SYSTEM_COMPROMISE -> Severity.CRITICAL
            ALERT_DATA_EXFILTRATION -> Severity.HIGH
            ALERT_CERTIFICATE_VIOLATION -> Severity.MEDIUM
            ALERT_PERMISSION_ESCALATION -> Severity.HIGH
            "account_locked" -> Severity.MEDIUM
            "connection_attempt" -> if (details["successful"] == false) Severity.LOW else Severity.LOW
            else -> Severity.LOW
        }
    }

    private fun generateAlertDescription(
        alertType: String,
        details: Map<String, Any>,
    ): String {
        return when (alertType) {
            ALERT_BRUTE_FORCE -> "Brute force attack detected: ${details["failed_attempts"]} failed attempts"
            ALERT_SUSPICIOUS_CONNECTION -> "Suspicious connection pattern: ${details["connections_per_minute"]} connections/minute"
            ALERT_UNUSUAL_ACTIVITY -> "Unusual activity detected: ${details["activity_count"]} actions in ${details["time_window_seconds"]}s"
            ALERT_SESSION_HIJACK -> "Potential session hijacking detected"
            ALERT_CERTIFICATE_VIOLATION -> "Certificate validation violation"
            ALERT_PERMISSION_ESCALATION -> "Unauthorized permission escalation attempt"
            ALERT_DATA_EXFILTRATION -> "Potential data exfiltration detected"
            ALERT_SYSTEM_COMPROMISE -> "System compromise indicators detected"
            else -> "Security event: $alertType"
        }
    }

    private fun generateAlertId(): String {
        return "ALERT_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }

    private fun getRecentFailedLogins(
        deviceId: String,
        timeWindowMs: Long,
    ): List<Long> {
        val cutoffTime = System.currentTimeMillis() - timeWindowMs
        return failedLogins[deviceId]?.filter { it > cutoffTime } ?: emptyList()
    }

    private fun getRecentAlerts(timeWindowMs: Long): List<SecurityAlert> {
        val cutoffTime = System.currentTimeMillis() - timeWindowMs
        synchronized(securityAlerts) {
            return securityAlerts.filter { it.timestamp > cutoffTime }
        }
    }

    private fun performCleanup() {
        val currentTime = System.currentTimeMillis()
        val cleanupCutoff = currentTime - (24 * 60 * 60 * 1000L)
        connectionAttempts.values.forEach { attempts ->
            attempts.removeAll { it < cleanupCutoff }
        }
        failedLogins.values.forEach { failures ->
            failures.removeAll { it < cleanupCutoff }
        }
        val inactiveSessions =
            sessionActivities.filterValues {
                currentTime - it.lastActivity > (60 * 60 * 1000L)
            }.keys
        inactiveSessions.forEach { deviceId ->
            sessionActivities.remove(deviceId)
        }
        logger.log(
            StructuredLogger.LogLevel.DEBUG,
            TAG,
            "cleanup_performed",
            mapOf(
                "inactive_sessions_removed" to inactiveSessions.size,
            ),
        )
    }

    private fun updateMonitoringStatistics() {
    }

    fun getSecurityAlerts(limit: Int = 100): List<SecurityAlert> {
        synchronized(securityAlerts) {
            return securityAlerts.takeLast(limit)
        }
    }

    fun acknowledgeAlert(alertId: String): Boolean {
        synchronized(securityAlerts) {
            val alert = securityAlerts.find { it.id == alertId }
            return if (alert != null) {
                alert.acknowledged = true
                logger.log(
                    StructuredLogger.LogLevel.INFO,
                    TAG,
                    "alert_acknowledged",
                    mapOf(
                        "alert_id" to alertId,
                    ),
                )
                true
            } else {
                false
            }
        }
    }

    fun getMonitoringStatistics(): JSONObject {
        return JSONObject().apply {
            put("monitoring_active", isMonitoring.get())
            put("total_connections", totalConnections.get())
            put("total_failed_logins", totalFailedLogins.get())
            put("total_security_alerts", totalSecurityAlerts.get())
            put("active_sessions", sessionActivities.size)
            put("recent_alerts_count", getRecentAlerts(60 * 60 * 1000L).size)
            put("monitored_devices", connectionAttempts.size)
        }
    }

    fun getSecurityDiagnostics(): JSONObject {
        return JSONObject().apply {
            put("monitoring_statistics", getMonitoringStatistics())
            put(
                "recent_alerts",
                getSecurityAlerts(10).map { alert ->
                    JSONObject().apply {
                        put("id", alert.id)
                        put("type", alert.type)
                        put("severity", alert.severity.name)
                        put("device_id", alert.deviceId)
                        put("timestamp", alert.timestamp)
                        put("acknowledged", alert.acknowledged)
                    }
                },
            )
            put(
                "active_sessions",
                sessionActivities.values.map { session ->
                    JSONObject().apply {
                        put("device_id", session.deviceId)
                        put("activity_count", session.activityCount)
                        put("suspicious_events", session.suspiciousEvents)
                        put("last_activity", session.lastActivity)
                    }
                },
            )
        }
    }
}
