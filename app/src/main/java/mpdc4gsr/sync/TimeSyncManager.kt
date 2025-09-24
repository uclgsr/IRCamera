package mpdc4gsr.sync

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Dedicated TimeSyncManager for handling NTP-style time synchronization between
 * Android device and PC as specified in the time synchronization implementation plan.
 * 
 * This manager handles:
 * - NTP-style timestamp exchange protocol
 * - Clock offset and RTT calculations
 * - Dedicated sync logging to CSV files
 * - Integration with recording lifecycle
 * - Non-blocking execution
 */
class TimeSyncManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TimeSyncManager"
        private const val SYNC_LOG_FILENAME = "timesync_log.csv"
        private const val CSV_HEADER = "sync_index,timestamp_iso,phone_timestamp_t2,pc_send_time_t1,pc_recv_time_t3,offset_ms,rtt_ms,session_relative_time_ms"
        
        // Default timeout for sync operations
        private const val SYNC_TIMEOUT_MS = 5000L
    }
    
    data class SyncResult(
        val success: Boolean,
        val t1: Long = 0L, // PC send time
        val t2: Long = 0L, // Phone receive time
        val t3: Long = 0L, // PC receive time (if provided)
        val offsetMs: Long = 0L,
        val rttMs: Long = 0L,
        val syncIndex: Int = 0
    )
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncCounter = AtomicLong(0)
    private var sessionStartTime: Long = 0L
    private var currentSessionDirectory: String? = null
    private var syncLogFile: File? = null
    
    /**
     * Initialize sync manager for a new session
     */
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
                Log.i(TAG, "Initialized sync logging for session: $sessionDirectory")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize sync log file", e)
            }
        }
    }
    
    /**
     * Perform immediate sync response - captures t2 timestamp and responds
     * This is called when receiving a SYNC_REQUEST from PC
     */
    suspend fun performSyncResponse(t1PcSendTime: Long): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                // Capture t2 immediately - phone's timestamp when receiving PC request
                val t2PhoneTimestamp = System.currentTimeMillis()
                
                Log.d(TAG, "Sync response: t1=$t1PcSendTime, t2=$t2PhoneTimestamp")
                
                val syncIndex = syncCounter.incrementAndGet().toInt()
                
                SyncResult(
                    success = true,
                    t1 = t1PcSendTime,
                    t2 = t2PhoneTimestamp,
                    syncIndex = syncIndex
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform sync response", e)
                SyncResult(success = false)
            }
        }
    }
    
    /**
     * Complete sync calculation when PC provides final offset and RTT data
     * This handles the optional PC->Phone offset message from the protocol
     */
    suspend fun completeSyncCalculation(
        t1: Long,
        t2: Long, 
        t3: Long,
        offsetMs: Long,
        rttMs: Long,
        syncIndex: Int
    ) {
        syncScope.launch {
            logSyncResult(
                SyncResult(
                    success = true,
                    t1 = t1,
                    t2 = t2,
                    t3 = t3,
                    offsetMs = offsetMs,
                    rttMs = rttMs,
                    syncIndex = syncIndex
                )
            )
        }
    }
    
    /**
     * Log sync result to dedicated CSV file
     */
    private suspend fun logSyncResult(result: SyncResult) {
        try {
            val logFile = syncLogFile
            if (logFile == null) {
                Log.w(TAG, "No sync log file initialized, skipping log")
                return
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val sessionRelativeTime = System.currentTimeMillis() - sessionStartTime
            
            val logEntry = "${result.syncIndex},$timestamp,${result.t2},${result.t1},${result.t3},${result.offsetMs},${result.rttMs},$sessionRelativeTime"
            
            FileWriter(logFile, true).use { writer ->
                writer.write("$logEntry\n")
            }
            
            Log.d(TAG, "Logged sync result: index=${result.syncIndex}, offset=${result.offsetMs}ms, rtt=${result.rttMs}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log sync result", e)
        }
    }
    
    /**
     * Perform sync at session start - this is called automatically when recording starts
     */
    suspend fun performSessionStartSync(): Boolean {
        return try {
            Log.i(TAG, "Performing session start sync")
            
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
            
            Log.i(TAG, "Session start sync marker logged")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform session start sync", e)
            false
        }
    }
    
    /**
     * Get sync log file for current session
     */
    fun getSyncLogFile(): File? = syncLogFile
    
    /**
     * Get current sync statistics
     */
    fun getSyncStats(): Map<String, Any> {
        return mapOf(
            "total_syncs" to syncCounter.get(),
            "session_directory" to (currentSessionDirectory ?: "none"),
            "session_start_time" to sessionStartTime,
            "sync_log_exists" to (syncLogFile?.exists() == true)
        )
    }
    
    /**
     * Finalize session and cleanup resources
     */
    fun finalizeSession() {
        try {
            currentSessionDirectory = null
            syncLogFile = null
            sessionStartTime = 0L
            
            Log.i(TAG, "Session finalized, total syncs: ${syncCounter.get()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing session", e)
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        syncScope.cancel()
        finalizeSession()
        Log.i(TAG, "TimeSyncManager cleaned up")
    }
}