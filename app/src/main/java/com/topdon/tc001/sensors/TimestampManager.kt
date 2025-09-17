package com.topdon.tc001.sensors

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

object TimestampManager {
    private const val TAG = "TimestampManager"

    private val bootTimeReference = AtomicLong(0L)
    private val clockOffset = AtomicLong(0L)
    private val sessionStartTime = AtomicLong(0L)
    
    // Session reference timestamps for cross-sensor alignment
    private val sessionStartSystemMs = AtomicLong(0L)
    private val sessionStartMonotonicNs = AtomicLong(0L)

    init {
        initializeTimestampSystem()
    }

    private fun initializeTimestampSystem() {
        bootTimeReference.set(System.currentTimeMillis() - SystemClock.elapsedRealtime())
        Log.i(TAG, "Timestamp system initialized with boot reference: ${bootTimeReference.get()}")
    }

    /**
     * High-precision nanosecond timestamp using monotonic clock
     * Use for sensor data that requires precise relative timing
     */
    fun nowNanos(): Long {
        return System.nanoTime()
    }

    /**
     * More precise monotonic clock for relative timing between sensors
     * Preferred over System.nanoTime() for consistency
     */
    fun getCurrentTimestampNanos(): Long {
        return SystemClock.elapsedRealtimeNanos()
    }

    /**
     * Wall clock time in milliseconds
     * Use for data export and human-readable timestamps
     */
    fun getCurrentSystemTimeMs(): Long {
        return System.currentTimeMillis()
    }

    fun getCurrentElapsedRealtimeMs(): Long {
        return SystemClock.elapsedRealtime()
    }

    fun getDeviceTimestampMs(): Long {
        return bootTimeReference.get() + SystemClock.elapsedRealtime()
    }

    fun getSessionRelativeTimestampMs(): Long {
        val sessionStart = sessionStartTime.get()
        if (sessionStart == 0L) {
            Log.w(TAG, "Session not started, returning absolute timestamp")
            return getCurrentElapsedRealtimeMs()
        }
        return getCurrentElapsedRealtimeMs() - sessionStart
    }

    /**
     * Start a new recording session and establish reference timestamps
     * Returns session reference data for cross-sensor synchronization
     */
    fun startSession(): SessionTimestampReference {
        val sessionStart = getCurrentElapsedRealtimeMs()
        val systemStart = getCurrentSystemTimeMs()
        val monotonicStart = getCurrentTimestampNanos()
        
        sessionStartTime.set(sessionStart)
        sessionStartSystemMs.set(systemStart)
        sessionStartMonotonicNs.set(monotonicStart)
        
        val reference = SessionTimestampReference(
            sessionStartElapsedMs = sessionStart,
            sessionStartSystemMs = systemStart,
            sessionStartMonotonicNs = monotonicStart,
            bootTimeReferenceMs = bootTimeReference.get()
        )
        
        Log.i(TAG, "Session started with reference: system=${systemStart}ms, monotonic=${monotonicStart}ns")
        return reference
    }

    fun endSession(): Long {
        val sessionEnd = getCurrentElapsedRealtimeMs()
        val sessionDuration = sessionEnd - sessionStartTime.get()
        sessionStartTime.set(0L)
        sessionStartSystemMs.set(0L)
        sessionStartMonotonicNs.set(0L)
        Log.i(TAG, "Session ended. Duration: $sessionDuration ms")
        return sessionDuration
    }

    fun setClockOffset(offsetMs: Long) {
        clockOffset.set(offsetMs)
        Log.i(TAG, "Clock offset set to: $offsetMs ms")
    }

    fun getSynchronizedTimestampMs(): Long {
        return getDeviceTimestampMs() + clockOffset.get()
    }

    /**
     * Create a comprehensive timestamp record with all timing references
     * This ensures all sensors can use a common time reference
     */
    fun createTimestampRecord(): TimestampRecord {
        val currentNanos = getCurrentTimestampNanos()
        val systemMs = getCurrentSystemTimeMs()
        val elapsedMs = getCurrentElapsedRealtimeMs()
        val deviceMs = getDeviceTimestampMs()
        val sessionRelativeMs = getSessionRelativeTimestampMs()
        val synchronizedMs = getSynchronizedTimestampMs()

        return TimestampRecord(
            systemNanos = currentNanos,
            systemTimeMs = systemMs,
            elapsedRealtimeMs = elapsedMs,
            deviceTimestampMs = deviceMs,
            sessionRelativeMs = sessionRelativeMs,
            synchronizedTimestampMs = synchronizedMs,
        )
    }

    /**
     * Convert monotonic nanosecond timestamp to system time for cross-sensor alignment
     */
    fun convertMonotonicToSystemTime(monotonicNs: Long): Long {
        val sessionStartMono = sessionStartMonotonicNs.get()
        val sessionStartSys = sessionStartSystemMs.get()
        
        if (sessionStartMono == 0L) {
            Log.w(TAG, "No session reference available for timestamp conversion")
            return getCurrentSystemTimeMs()
        }
        
        val offsetNs = monotonicNs - sessionStartMono
        val offsetMs = offsetNs / 1_000_000
        return sessionStartSys + offsetMs
    }

    /**
     * Get session-relative timestamp in nanoseconds from monotonic clock
     */
    fun getSessionRelativeNanos(currentMonotonicNs: Long = getCurrentTimestampNanos()): Long {
        val sessionStartMono = sessionStartMonotonicNs.get()
        if (sessionStartMono == 0L) {
            Log.w(TAG, "No session started for relative timestamp")
            return currentMonotonicNs
        }
        return currentMonotonicNs - sessionStartMono
    }

    inline fun <T> measureExecutionTime(block: () -> T): Pair<T, Long> {
        val executionTime = measureNanoTime {
            return block() to 0L
        }
        return Pair(block(), executionTime)
    }
}

/**
 * Session reference timestamps for cross-sensor synchronization
 */
data class SessionTimestampReference(
    val sessionStartElapsedMs: Long,
    val sessionStartSystemMs: Long, 
    val sessionStartMonotonicNs: Long,
    val bootTimeReferenceMs: Long
) {
    fun toCsvMetadata(): String {
        return "# Session Reference Timestamps\n" +
               "# session_start_elapsed_ms=$sessionStartElapsedMs\n" +
               "# session_start_system_ms=$sessionStartSystemMs\n" +
               "# session_start_monotonic_ns=$sessionStartMonotonicNs\n" +
               "# boot_time_reference_ms=$bootTimeReferenceMs\n"
    }
}

data class TimestampRecord(
    val systemNanos: Long, // High-precision nanosecond timestamp (for GSR samples)
    val systemTimeMs: Long, // Wall clock time (for human-readable timestamps)
    val elapsedRealtimeMs: Long, // System elapsed time (for sensor alignment)
    val deviceTimestampMs: Long, // Absolute device time (for data export)
    val sessionRelativeMs: Long, // Session-relative time (for analysis)
    val synchronizedTimestampMs: Long, // Hub-synchronized time (for multi-device sessions)
) {

    fun toCsvFormat(): String {
        return "$systemNanos,$systemTimeMs,$elapsedRealtimeMs,$deviceTimestampMs,$sessionRelativeMs,$synchronizedTimestampMs"
    }

    companion object {
        fun getCsvHeader(): String {
            return "system_nanos,system_time_ms,elapsed_realtime_ms,device_timestamp_ms,session_relative_ms,synchronized_timestamp_ms"
        }
    }
}
