package com.topdon.tc001.sensors

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

/**
 * Unified timestamp manager for cross-sensor synchronization
 * Provides high-precision timestamps for GSR, thermal, and RGB sensors
 * with proper alignment for multi-modal data analysis
 */
object TimestampManager {
    private const val TAG = "TimestampManager"

    // System boot time reference for consistent timestamps
    private val bootTimeReference = AtomicLong(0L)

    // Cross-device clock offset for hub-spoke synchronization
    private val clockOffset = AtomicLong(0L)

    // Session start time for relative timestamps
    private val sessionStartTime = AtomicLong(0L)

    init {
        initializeTimestampSystem()
    }

    /**
     * Initialize the timestamp system with boot time reference
     */
    private fun initializeTimestampSystem() {
        bootTimeReference.set(System.currentTimeMillis() - SystemClock.elapsedRealtime())
        Log.i(TAG, "Timestamp system initialized with boot reference: ${bootTimeReference.get()}")
    }

    /**
     * Get current system timestamp in nanoseconds
     * Used for high-precision GSR sample timestamping
     */
    fun getCurrentTimestampNanos(): Long {
        return System.nanoTime()
    }

    /**
     * Get current elapsed realtime in milliseconds
     * Used for cross-sensor alignment (compatible with RGB/thermal timestamps)
     */
    fun getCurrentElapsedRealtimeMs(): Long {
        return SystemClock.elapsedRealtime()
    }

    /**
     * Get device timestamp in milliseconds
     * Combines system elapsed time with boot reference for absolute timestamp
     */
    fun getDeviceTimestampMs(): Long {
        return bootTimeReference.get() + SystemClock.elapsedRealtime()
    }

    /**
     * Get session-relative timestamp for cross-sensor data alignment
     * Returns milliseconds since session start
     */
    fun getSessionRelativeTimestampMs(): Long {
        val sessionStart = sessionStartTime.get()
        if (sessionStart == 0L) {
            Log.w(TAG, "Session not started, returning absolute timestamp")
            return getCurrentElapsedRealtimeMs()
        }
        return getCurrentElapsedRealtimeMs() - sessionStart
    }

    /**
     * Start a new recording session and set session reference time
     */
    fun startSession(): Long {
        val sessionStart = getCurrentElapsedRealtimeMs()
        sessionStartTime.set(sessionStart)
        Log.i(TAG, "Session started at: $sessionStart ms")
        return sessionStart
    }

    /**
     * End the current recording session
     */
    fun endSession(): Long {
        val sessionEnd = getCurrentElapsedRealtimeMs()
        val sessionDuration = sessionEnd - sessionStartTime.get()
        sessionStartTime.set(0L)
        Log.i(TAG, "Session ended. Duration: $sessionDuration ms")
        return sessionDuration
    }

    /**
     * Set clock offset for hub-spoke synchronization
     * Offset in milliseconds to align with PC hub time
     */
    fun setClockOffset(offsetMs: Long) {
        clockOffset.set(offsetMs)
        Log.i(TAG, "Clock offset set to: $offsetMs ms")
    }

    /**
     * Get synchronized timestamp for hub-spoke data alignment
     * Applies clock offset for consistency with PC hub
     */
    fun getSynchronizedTimestampMs(): Long {
        return getDeviceTimestampMs() + clockOffset.get()
    }

    /**
     * Create a timestamp record for GSR data persistence
     * Returns a formatted record with multiple timestamp formats for compatibility
     */
    fun createTimestampRecord(): TimestampRecord {
        val currentNanos = getCurrentTimestampNanos()
        val elapsedMs = getCurrentElapsedRealtimeMs()
        val deviceMs = getDeviceTimestampMs()
        val sessionRelativeMs = getSessionRelativeTimestampMs()
        val synchronizedMs = getSynchronizedTimestampMs()

        return TimestampRecord(
            systemNanos = currentNanos,
            elapsedRealtimeMs = elapsedMs,
            deviceTimestampMs = deviceMs,
            sessionRelativeMs = sessionRelativeMs,
            synchronizedTimestampMs = synchronizedMs,
        )
    }

    /**
     * Measure execution time of a block in nanoseconds
     * Used for performance monitoring and precision timing
     */
    inline fun <T> measureExecutionTime(block: () -> T): Pair<T, Long> {
        val executionTime =
            measureNanoTime {
                return block() to 0L
            }
        return Pair(block(), executionTime)
    }
}

/**
 * Comprehensive timestamp record for cross-sensor data alignment
 * Contains multiple timestamp formats for maximum compatibility
 */
data class TimestampRecord(
    val systemNanos: Long, // High-precision nanosecond timestamp (for GSR samples)
    val elapsedRealtimeMs: Long, // System elapsed time (for sensor alignment)
    val deviceTimestampMs: Long, // Absolute device time (for data export)
    val sessionRelativeMs: Long, // Session-relative time (for analysis)
    val synchronizedTimestampMs: Long, // Hub-synchronized time (for multi-device sessions)
) {
    /**
     * Convert to CSV format for data export
     * Compatible with research analysis tools
     */
    fun toCsvFormat(): String {
        return "$systemNanos,$elapsedRealtimeMs,$deviceTimestampMs,$sessionRelativeMs,$synchronizedTimestampMs"
    }

    /**
     * Get CSV header for timestamp columns
     */
    companion object {
        fun getCsvHeader(): String {
            return "system_nanos,elapsed_realtime_ms,device_timestamp_ms,session_relative_ms,synchronized_timestamp_ms"
        }
    }
}
