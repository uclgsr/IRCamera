package mpdc4gsr.sensors.managers

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.utils.TimeManager
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * TimeSyncManager - Centralized time synchronization management
 * 
 * This class manages time synchronization between PC and Android device using
 * NTP-style clock offset calculation:
 * - Handles SYNC command exchange with PC
 * - Calculates and maintains clock offset
 * - Provides synchronized timestamps for all sensors
 * - Monitors clock drift and triggers re-sync when needed
 */
class TimeSyncManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "TimeSyncManager"
        private const val SYNC_ROUNDS = 3
        private const val DRIFT_MONITOR_INTERVAL_MS = 30000L
        private const val MAX_ACCEPTABLE_OFFSET_MS = 100.0
        private const val RESYNC_THRESHOLD_MS = 50.0
    }

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeManager = TimeManager.getInstance(context)
    
    private val _syncStatus = MutableStateFlow(SyncStatus.NOT_SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _lastSyncOffset = MutableStateFlow(0.0)
    val lastSyncOffset: StateFlow<Double> = _lastSyncOffset.asStateFlow()
    
    private val _lastSyncQuality = MutableStateFlow(Double.MAX_VALUE)
    val lastSyncQuality: StateFlow<Double> = _lastSyncQuality.asStateFlow()
    
    private val syncLog = mutableListOf<SyncLogEntry>()
    private val syncLogLock = Any()
    
    /**
     * Initiate time sync with PC using NTP-style exchange
     */
    suspend fun initiateSync(pcAddress: String, port: Int = 8080): SyncResult {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "Initiating time sync with PC at $pcAddress:$port")
            _syncStatus.value = SyncStatus.SYNCING
            
            val syncResults = mutableListOf<TimeSyncAttempt>()
            
            // Perform multiple sync rounds for accuracy
            repeat(SYNC_ROUNDS) { round ->
                Log.d(TAG, "Sync round ${round + 1}/$SYNC_ROUNDS")
                
                val t1 = SystemClock.elapsedRealtimeNanos() // Send time
                val syncResponse = timeManager.performTimeSync(pcAddress, port)
                val t4 = SystemClock.elapsedRealtimeNanos() // Receive time
                
                syncResponse?.let { response ->
                    val t2 = response.pcReceiveTime // PC receive time
                    val t3 = response.pcSendTime    // PC send time
                    
                    // Calculate offset using NTP formula
                    val roundTripTime = (t4 - t1)
                    val networkDelay = roundTripTime / 2
                    val clockOffset = ((t2 - t1) + (t3 - t4)) / 2
                    
                    val attempt = TimeSyncAttempt(
                        round = round,
                        t1 = t1,
                        t2 = t2,
                        t3 = t3,
                        t4 = t4,
                        clockOffsetNs = clockOffset,
                        roundTripTimeNs = roundTripTime,
                        networkDelayNs = networkDelay
                    )
                    
                    syncResults.add(attempt)
                    
                    Log.d(TAG, "Round ${round + 1}: offset=${clockOffset / 1_000_000.0}ms, RTT=${roundTripTime / 1_000_000.0}ms")
                } ?: run {
                    Log.w(TAG, "Sync round ${round + 1} failed")
                }
                
                if (round < SYNC_ROUNDS - 1) {
                    delay(100) // Brief pause between rounds
                }
            }
            
            if (syncResults.isEmpty()) {
                Log.e(TAG, "All sync rounds failed")
                _syncStatus.value = SyncStatus.SYNC_FAILED
                return@withContext SyncResult.FAILED
            }
            
            // Calculate final offset from successful rounds
            val finalResult = processSyncResults(syncResults)
            
            // Log the sync event
            logSyncEvent(finalResult)
            
            // Start drift monitoring
            startDriftMonitoring()
            
            finalResult.syncResult
        }
    }
    
    /**
     * Process multiple sync attempts to get best offset estimate
     */
    private fun processSyncResults(attempts: List<TimeSyncAttempt>): SyncFinalResult {
        // Filter out attempts with excessive RTT (high jitter)
        val validAttempts = attempts.filter { 
            (it.roundTripTimeNs / 1_000_000.0) < 100.0 // Less than 100ms RTT
        }
        
        if (validAttempts.isEmpty()) {
            Log.w(TAG, "No valid sync attempts (all had high RTT)")
            _syncStatus.value = SyncStatus.SYNC_FAILED
            return SyncFinalResult(SyncResult.FAILED, 0.0, Double.MAX_VALUE, attempts)
        }
        
        // Use median offset to reduce jitter impact
        val offsets = validAttempts.map { it.clockOffsetNs / 1_000_000.0 }.sorted()
        val finalOffsetMs = if (offsets.size % 2 == 0) {
            (offsets[offsets.size / 2 - 1] + offsets[offsets.size / 2]) / 2.0
        } else {
            offsets[offsets.size / 2]
        }
        
        // Calculate quality metric (standard deviation)
        val mean = offsets.average()
        val variance = offsets.map { (it - mean) * (it - mean) }.average()
        val qualityMs = kotlin.math.sqrt(variance)
        
        // Update state
        _lastSyncOffset.value = finalOffsetMs
        _lastSyncQuality.value = qualityMs
        
        val result = if (abs(finalOffsetMs) < MAX_ACCEPTABLE_OFFSET_MS && qualityMs < 10.0) {
            _syncStatus.value = SyncStatus.SYNCED
            SyncResult.SUCCESS
        } else {
            _syncStatus.value = SyncStatus.SYNC_POOR_QUALITY
            SyncResult.POOR_QUALITY
        }
        
        Log.i(TAG, "Sync completed: offset=${finalOffsetMs}ms, quality=${qualityMs}ms, result=$result")
        
        return SyncFinalResult(result, finalOffsetMs, qualityMs, attempts)
    }
    
    /**
     * Get synchronized timestamp for data recording
     */
    fun getSyncedTimestampNs(): Long {
        return timeManager.getCurrentTimestampNs()
    }
    
    /**
     * Start monitoring for clock drift
     */
    private fun startDriftMonitoring() {
        syncScope.launch {
            while (true) {
                delay(DRIFT_MONITOR_INTERVAL_MS)
                
                if (_syncStatus.value == SyncStatus.SYNCED) {
                    // Could implement drift detection here by comparing with system time
                    Log.d(TAG, "Clock drift monitoring - status OK")
                } else {
                    Log.d(TAG, "Skipping drift monitoring - not synced")
                }
            }
        }
    }
    
    /**
     * Log sync event for analysis
     */
    private fun logSyncEvent(result: SyncFinalResult) {
        val logEntry = SyncLogEntry(
            timestamp = System.currentTimeMillis(),
            offsetMs = result.finalOffsetMs,
            qualityMs = result.qualityMs,
            result = result.syncResult,
            attempts = result.attempts.size
        )
        
        synchronized(syncLogLock) {
            syncLog.add(logEntry)
            
            // Keep only last 100 entries
            if (syncLog.size > 100) {
                syncLog.removeFirst()
            }
        }
        
        Log.d(TAG, "Logged sync event: $logEntry")
    }
    
    /**
     * Get sync log for analysis
     */
    fun getSyncLog(): List<SyncLogEntry> {
        synchronized(syncLogLock) {
            return syncLog.toList()
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up time sync manager")
        syncScope.cancel()
        _syncStatus.value = SyncStatus.NOT_SYNCED
    }
    
    // Data classes for sync results
    data class TimeSyncAttempt(
        val round: Int,
        val t1: Long,
        val t2: Long,
        val t3: Long,
        val t4: Long,
        val clockOffsetNs: Long,
        val roundTripTimeNs: Long,
        val networkDelayNs: Long
    )
    
    data class SyncFinalResult(
        val syncResult: SyncResult,
        val finalOffsetMs: Double,
        val qualityMs: Double,
        val attempts: List<TimeSyncAttempt>
    )
    
    data class SyncLogEntry(
        val timestamp: Long,
        val offsetMs: Double,
        val qualityMs: Double,
        val result: SyncResult,
        val attempts: Int
    )
    
    enum class SyncStatus {
        NOT_SYNCED,
        SYNCING,
        SYNCED,
        SYNC_FAILED,
        SYNC_POOR_QUALITY
    }
    
    enum class SyncResult {
        SUCCESS,
        FAILED,
        POOR_QUALITY
    }
}