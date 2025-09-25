package com.mpdc4gsr.libunified.app.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Consolidated time utilities replacing scattered time management classes
 * Provides centralized time handling for the application
 */
object UnifiedTimeUtils {

    // Common date/time formats
    private const val FORMAT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss"
    private const val FORMAT_DATE = "yyyy-MM-dd"
    private const val FORMAT_TIME = "HH:mm:ss"
    private const val FORMAT_FILENAME = "yyyyMMdd_HHmmss"
    private const val FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    /**
     * Get current timestamp as string
     */
    fun getCurrentTimestamp(): String {
        return SimpleDateFormat(FORMAT_TIMESTAMP, Locale.getDefault()).format(Date())
    }

    /**
     * Get current date as string
     */
    fun getCurrentDate(): String {
        return SimpleDateFormat(FORMAT_DATE, Locale.getDefault()).format(Date())
    }

    /**
     * Get current time as string
     */
    fun getCurrentTime(): String {
        return SimpleDateFormat(FORMAT_TIME, Locale.getDefault()).format(Date())
    }

    /**
     * Get filename-safe timestamp
     */
    fun getFilenameTimestamp(): String {
        return SimpleDateFormat(FORMAT_FILENAME, Locale.getDefault()).format(Date())
    }

    /**
     * Get ISO formatted timestamp
     */
    fun getISOTimestamp(): String {
        return SimpleDateFormat(FORMAT_ISO, Locale.getDefault()).format(Date())
    }

    /**
     * Format timestamp with custom format
     */
    fun formatTimestamp(timestamp: Long, format: String): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Format date with custom format
     */
    fun formatDate(date: Date, format: String): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(date)
    }

    /**
     * Parse timestamp string to Date
     */
    fun parseTimestamp(timestamp: String, format: String = FORMAT_TIMESTAMP): Date? {
        return try {
            SimpleDateFormat(format, Locale.getDefault()).parse(timestamp)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get milliseconds since epoch
     */
    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Get nanoseconds since epoch (system time)
     */
    fun getCurrentTimeNanos(): Long {
        return System.nanoTime()
    }

    /**
     * Convert milliseconds to readable duration
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m"
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if timestamp is within last N days
     */
    fun isWithinDays(timestamp: Long, days: Int): Boolean {
        val cutoff = getCurrentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return timestamp >= cutoff
    }

    /**
     * Get age in milliseconds
     */
    fun getAge(timestamp: Long): Long {
        return getCurrentTimeMillis() - timestamp
    }

    /**
     * Sleep for specified milliseconds
     */
    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}