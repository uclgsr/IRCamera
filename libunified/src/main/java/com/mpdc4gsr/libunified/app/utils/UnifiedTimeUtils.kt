package com.mpdc4gsr.libunified.app.utils

import java.text.SimpleDateFormat
import java.util.*

object UnifiedTimeUtils {
    // Common date/time formats
    private const val FORMAT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss"
    private const val FORMAT_DATE = "yyyy-MM-dd"
    private const val FORMAT_TIME = "HH:mm:ss"
    private const val FORMAT_FILENAME = "yyyyMMdd_HHmmss"
    private const val FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    fun getCurrentTimestamp(): String = SimpleDateFormat(FORMAT_TIMESTAMP, Locale.getDefault()).format(Date())

    fun getCurrentDate(): String = SimpleDateFormat(FORMAT_DATE, Locale.getDefault()).format(Date())

    fun getCurrentTime(): String = SimpleDateFormat(FORMAT_TIME, Locale.getDefault()).format(Date())

    fun getFilenameTimestamp(): String = SimpleDateFormat(FORMAT_FILENAME, Locale.getDefault()).format(Date())

    fun getISOTimestamp(): String = SimpleDateFormat(FORMAT_ISO, Locale.getDefault()).format(Date())

    fun formatTimestamp(
        timestamp: Long,
        format: String,
    ): String = SimpleDateFormat(format, Locale.getDefault()).format(Date(timestamp))

    fun formatDate(
        date: Date,
        format: String,
    ): String = SimpleDateFormat(format, Locale.getDefault()).format(date)

    fun parseTimestamp(
        timestamp: String,
        format: String = FORMAT_TIMESTAMP,
    ): Date? =
        try {
            SimpleDateFormat(format, Locale.getDefault()).parse(timestamp)
        } catch (e: Exception) {
            null
        }

    fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

    fun getCurrentTimeNanos(): Long = System.nanoTime()

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

    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    fun isWithinDays(
        timestamp: Long,
        days: Int,
    ): Boolean {
        val cutoff = getCurrentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return timestamp >= cutoff
    }

    fun getAge(timestamp: Long): Long = getCurrentTimeMillis() - timestamp

    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
