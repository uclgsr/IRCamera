package mpdc4gsr.infrastructure.data

import android.content.Context
import android.util.Log
import mpdc4gsr.data.utils.TimeManager
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class TimeSyncManager(private val context: Context) {
    private val timeManager = TimeManager.getInstance(context)

    companion object {
        private const val TAG = "TimeSyncManager"
        private const val SYNC_LOG_FILENAME = "timesync_log.csv"
        private const val CSV_HEADER =
            "sync_index,timestamp_iso,phone_timestamp_t2,pc_send_time_t1,pc_recv_time_t3,offset_ms,rtt_ms,session_relative_time_ms,sync_quality,retry_count"

        // Default timeout for sync operations
        private const val SYNC_TIMEOUT_MS = 5000L

        // Periodic sync configuration
        private const val PERIODIC_SYNC_INTERVAL_MS = 300_000L // 5 minutes
        private const val LONG_SESSION_THRESHOLD_MS = 600_000L // 10 minutes

        // Timestamp validation constants
        private const val MAX_TIMESTAMP_DRIFT_MS = 86400_000L // 24 hours
        private const val MAX_FUTURE_TIMESTAMP_MS = 300_000L // 5 minutes in future

        // Retry logic constants
        private const val MAX_SYNC_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L

        // Sync quality thresholds
        private const val EXCELLENT_RTT_THRESHOLD_MS = 10L
        private const val GOOD_RTT_THRESHOLD_MS = 50L
        private const val FAIR_RTT_THRESHOLD_MS = 200L
    }

    enum class SyncQuality {
        EXCELLENT, GOOD, FAIR, POOR
    }

    data class SyncConfiguration(
        val periodicSyncIntervalMs: Long = PERIODIC_SYNC_INTERVAL_MS,
        val longSessionThresholdMs: Long = LONG_SESSION_THRESHOLD_MS,
        val maxSyncRetries: Int = MAX_SYNC_RETRIES,
        val syncTimeoutMs: Long = SYNC_TIMEOUT_MS,
        val retryDelayMs: Long = RETRY_DELAY_MS,
        val maxTimestampDriftMs: Long = MAX_TIMESTAMP_DRIFT_MS,
        val maxFutureTimestampMs: Long = MAX_FUTURE_TIMESTAMP_MS,
        val enableJsonLogging: Boolean = true,
        val enableCsvLogging: Boolean = true
    )

    data class SyncResult(
        val success: Boolean,
        val t1: Long = 0L, // PC send time
        val t2: Long = 0L, // Phone receive time
        val t3: Long = 0L, // PC receive time (if provided)
        val offsetMs: Long = 0L,
        val rttMs: Long = 0L,
        val syncIndex: Int = 0,
        val quality: SyncQuality = SyncQuality.POOR,
        val retryCount: Int = 0,
        val errorMessage: String? = null
    )

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncCounter = AtomicLong(0)
    private var sessionStartTime: Long = 0L
    private var currentSessionDirectory: String? = null
    private var syncLogFile: File? = null
    private val periodicSyncEnabled = AtomicBoolean(false)
    private var periodicSyncJob: kotlinx.coroutines.Job? = null

    // Configuration for sync behavior
    private var syncConfig = SyncConfiguration()

    // Callback interface for manual sync triggers
    interface SyncTriggerCallback {
        suspend fun onManualSyncRequested(): Boolean
    }

    private var syncTriggerCallback: SyncTriggerCallback? = null

    // Sync quality tracking
    private val syncQualityHistory = mutableListOf<Pair<Long, SyncQuality>>()
    private val maxQualityHistorySize = 100

    fun updateSyncConfiguration(config: SyncConfiguration) {
        syncConfig = config
        Log.i(
            TAG,
            "Sync configuration updated: periodicInterval=${config.periodicSyncIntervalMs}ms, " +
                    "maxRetries=${config.maxSyncRetries}, timeout=${config.syncTimeoutMs}ms"
        )
    }

    fun getSyncConfiguration(): SyncConfiguration = syncConfig

    private fun validateTimestamp(timestamp: Long, context: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeDiff = timestamp - currentTime
        return when {
            timeDiff > syncConfig.maxFutureTimestampMs -> {
                false
            }

            timeDiff < -syncConfig.maxTimestampDriftMs -> {
                false
            }

            else -> true
        }
    }

    private fun calculateSyncQuality(rttMs: Long, retryCount: Int): SyncQuality {
        return when {
            rttMs <= EXCELLENT_RTT_THRESHOLD_MS && retryCount == 0 -> SyncQuality.EXCELLENT
            rttMs <= GOOD_RTT_THRESHOLD_MS && retryCount <= 1 -> SyncQuality.GOOD
            rttMs <= FAIR_RTT_THRESHOLD_MS && retryCount <= 2 -> SyncQuality.FAIR
            else -> SyncQuality.POOR
        }
    }

    private fun updateSyncQualityHistory(quality: SyncQuality) {
        val timestamp = System.currentTimeMillis()
        syncQualityHistory.add(timestamp to quality)
        // Keep history size manageable
        if (syncQualityHistory.size > maxQualityHistorySize) {
            syncQualityHistory.removeAt(0)
        }
    }

    fun getSyncQualityMetrics(): Map<String, Any> {
        if (syncQualityHistory.isEmpty()) {
            return mapOf("total_syncs" to 0, "average_quality" to "UNKNOWN")
        }
        val qualityCounts = syncQualityHistory.groupingBy { it.second }.eachCount()
        val totalSyncs = syncQualityHistory.size
        val recentSyncs = syncQualityHistory.takeLast(10)
        return mapOf(
            "total_syncs" to totalSyncs,
            "excellent_count" to (qualityCounts[SyncQuality.EXCELLENT] ?: 0),
            "good_count" to (qualityCounts[SyncQuality.GOOD] ?: 0),
            "fair_count" to (qualityCounts[SyncQuality.FAIR] ?: 0),
            "poor_count" to (qualityCounts[SyncQuality.POOR] ?: 0),
            "recent_quality_trend" to recentSyncs.map { it.second.name }
        )
    }

    fun setSyncTriggerCallback(callback: SyncTriggerCallback) {
        syncTriggerCallback = callback
    }

    fun setPeriodicSyncEnabled(enabled: Boolean) {
        periodicSyncEnabled.set(enabled)
        if (enabled && currentSessionDirectory != null) {
            startPeriodicSync()
        } else {
            stopPeriodicSync()
        }
    }

    suspend fun triggerManualSync(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val callback = syncTriggerCallback
                if (callback != null) {
                    callback.onManualSyncRequested()
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun startPeriodicSync() {
        if (periodicSyncJob?.isActive == true) {
            return // Already running
        }
        periodicSyncJob = syncScope.launch {
            Log.i(
                TAG,
                "Starting periodic sync monitoring (interval: ${syncConfig.periodicSyncIntervalMs}ms)"
            )
            while (isActive && periodicSyncEnabled.get()) {
                delay(syncConfig.periodicSyncIntervalMs)
                if (currentSessionDirectory != null) {
                    val sessionDuration = System.currentTimeMillis() - sessionStartTime
                    if (sessionDuration > syncConfig.longSessionThresholdMs) {
                        Log.i(
                            TAG,
                            "Triggering periodic sync for long session (${sessionDuration / 1000}s)"
                        )
                        try {
                            triggerManualSync()
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }

    fun initializeSession(sessionDirectory: String) {
        currentSessionDirectory = sessionDirectory
        sessionStartTime = System.currentTimeMillis()
        // Create sync log file
        val sessionDir = File(sessionDirectory)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        syncLogFile = File(sessionDir, SYNC_LOG_FILENAME)
        // Write CSV header
        syncScope.launch {
            try {
                FileWriter(syncLogFile!!, false).use { writer ->
                    writer.write("$CSV_HEADER\n")
                }
            } catch (e: Exception) {
            }
        }
        // Start periodic sync if enabled
        if (periodicSyncEnabled.get()) {
            startPeriodicSync()
        }
    }

    suspend fun performSyncResponse(t1PcSendTime: Long): SyncResult {
        return withContext(Dispatchers.IO) {
            var retryCount = 0
            var lastError: String? = null
            repeat(syncConfig.maxSyncRetries) { attempt ->
                try {
                    // Validate PC timestamp
                    if (!validateTimestamp(t1PcSendTime, "PC sync request")) {
                        return@withContext SyncResult(
                            success = false,
                            errorMessage = "Invalid PC timestamp: $t1PcSendTime"
                        )
                    }
                    // Capture t2 immediately with timeout protection
                    val t2PhoneTimestamp = withTimeoutOrNull(syncConfig.syncTimeoutMs) {
                        System.currentTimeMillis()
                    } ?: run {
                        lastError = "Timeout capturing phone timestamp"
                        retryCount = attempt + 1
                        if (attempt < syncConfig.maxSyncRetries - 1) {
                            delay(syncConfig.retryDelayMs)
                        }
                        return@repeat
                    }
                    // Validate captured timestamp
                    if (!validateTimestamp(t2PhoneTimestamp, "Phone sync response")) {
                        lastError = "Invalid phone timestamp: $t2PhoneTimestamp"
                        retryCount = attempt + 1
                        if (attempt < syncConfig.maxSyncRetries - 1) {
                            delay(syncConfig.retryDelayMs)
                        }
                        return@repeat
                    }
                    Log.d(
                        TAG,
                        "Sync response: t1=$t1PcSendTime, t2=$t2PhoneTimestamp (attempt ${attempt + 1})"
                    )
                    val syncIndex = syncCounter.incrementAndGet().toInt()
                    return@withContext SyncResult(
                        success = true,
                        t1 = t1PcSendTime,
                        t2 = t2PhoneTimestamp,
                        syncIndex = syncIndex,
                        retryCount = retryCount
                    )
                } catch (e: Exception) {
                    lastError = "Sync response failed: ${e.message}"
                    retryCount = attempt + 1
                    if (attempt < syncConfig.maxSyncRetries - 1) {
                        delay(syncConfig.retryDelayMs)
                    }
                }
            }
            SyncResult(success = false, retryCount = retryCount, errorMessage = lastError)
        }
    }

    suspend fun completeSyncCalculation(
        t1: Long,
        t2: Long,
        t3: Long,
        offsetMs: Long,
        rttMs: Long,
        syncIndex: Int
    ) {
        var retryCount = 0
        repeat(syncConfig.maxSyncRetries) { attempt ->
            try {
                // Validate all timestamps
                if (!validateTimestamp(t1, "PC send time") ||
                    !validateTimestamp(t2, "Phone receive time") ||
                    !validateTimestamp(t3, "PC receive time")
                ) {
                    return
                }
                // Calculate sync quality
                val quality = calculateSyncQuality(rttMs, retryCount)
                updateSyncQualityHistory(quality)
                val result = SyncResult(
                    success = true,
                    t1 = t1,
                    t2 = t2,
                    t3 = t3,
                    offsetMs = offsetMs,
                    rttMs = rttMs,
                    syncIndex = syncIndex,
                    quality = quality,
                    retryCount = retryCount
                )
                // Apply clock offset to both TimeManager and TimestampManager
                // Don't catch exceptions here - let them propagate to trigger retry
                timeManager.setClockOffsetFromProtocolSync(offsetMs * 1_000_000, rttMs)
                TimestampManager.setClockOffset(offsetMs)
                // Attempt to log with retry logic
                val logged = withTimeoutOrNull(syncConfig.syncTimeoutMs) {
                    logSyncResult(result)
                    true
                } ?: false
                if (logged) {
                    Log.d(
                        TAG,
                        "Sync calculation completed successfully (quality: $quality, attempt: ${attempt + 1})"
                    )
                    return
                } else {
                    retryCount = attempt + 1
                    if (attempt < syncConfig.maxSyncRetries - 1) {
                        delay(syncConfig.retryDelayMs)
                    }
                }
            } catch (e: Exception) {
                retryCount = attempt + 1
                if (attempt < syncConfig.maxSyncRetries - 1) {
                    delay(syncConfig.retryDelayMs)
                }
            }
        }
    }

    private suspend fun logSyncResult(result: SyncResult) {
        try {
            val logFile = syncLogFile
            if (logFile == null) {
                return
            }
            val timestamp =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val sessionRelativeTime = System.currentTimeMillis() - sessionStartTime
            FileWriter(logFile, true).use { writer ->
                // Write JSON entry if enabled
                if (syncConfig.enableJsonLogging) {
                    val jsonEntry = """
                        {
                            "sync_index": ${result.syncIndex},
                            "timestamp_iso": "$timestamp",
                            "phone_timestamp_t2": ${result.t2},
                            "pc_send_time_t1": ${result.t1},
                            "pc_recv_time_t3": ${result.t3},
                            "offset_ms": ${result.offsetMs},
                            "rtt_ms": ${result.rttMs},
                            "session_relative_time_ms": $sessionRelativeTime,
                            "sync_quality": "${result.quality}",
                            "retry_count": ${result.retryCount},
                            "success": ${result.success}
                        }
                    """.trimIndent()
                    writer.write("// JSON: $jsonEntry\n")
                }
                // Write CSV entry if enabled (for backward compatibility)
                if (syncConfig.enableCsvLogging) {
                    val csvEntry =
                        "${result.syncIndex},$timestamp,${result.t2},${result.t1},${result.t3},${result.offsetMs},${result.rttMs},$sessionRelativeTime,${result.quality},${result.retryCount}"
                    writer.write("$csvEntry\n")
                }
            }
            Log.d(
                TAG,
                "Logged sync result: index=${result.syncIndex}, offset=${result.offsetMs}ms, rtt=${result.rttMs}ms, quality=${result.quality}"
            )
        } catch (e: Exception) {
            throw e // Re-throw to trigger retry logic
        }
    }

    suspend fun performSessionStartSync(): Boolean {
        return try {
            // Log a session start marker
            val sessionStartMarker = SyncResult(
                success = true,
                t1 = System.currentTimeMillis(),
                t2 = System.currentTimeMillis(),
                t3 = System.currentTimeMillis(),
                offsetMs = 0L,
                rttMs = 0L,
                syncIndex = 0
            )
            logSyncResult(sessionStartMarker)
            // Trigger actual sync with PC if callback is available
            val callback = syncTriggerCallback
            if (callback != null) {
                val syncTriggered = callback.onManualSyncRequested()
                if (syncTriggered) {
                } else {
                }
            } else {
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSyncLogFile(): File? = syncLogFile

    fun getSyncStats(): Map<String, Any> {
        val qualityMetrics = getSyncQualityMetrics()
        return mapOf(
            "total_syncs" to syncCounter.get(),
            "session_directory" to (currentSessionDirectory ?: "none"),
            "session_start_time" to sessionStartTime,
            "sync_log_exists" to (syncLogFile?.exists() == true),
            "periodic_sync_enabled" to periodicSyncEnabled.get(),
            "sync_quality_metrics" to qualityMetrics,
            "configuration" to mapOf(
                "periodic_interval_ms" to syncConfig.periodicSyncIntervalMs,
                "max_retries" to syncConfig.maxSyncRetries,
                "timeout_ms" to syncConfig.syncTimeoutMs,
                "json_logging_enabled" to syncConfig.enableJsonLogging,
                "csv_logging_enabled" to syncConfig.enableCsvLogging
            )
        )
    }

    fun finalizeSession() {
        try {
            // Stop periodic sync
            stopPeriodicSync()
            currentSessionDirectory = null
            syncLogFile = null
            sessionStartTime = 0L
        } catch (e: Exception) {
        }
    }

    fun cleanup() {
        stopPeriodicSync()
        syncScope.cancel()
        finalizeSession()
        syncTriggerCallback = null
    }
}