package com.topdon.tc001.util

import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CSV-specific buffered writer that extends BufferedDataWriter
 * Provides header management and CSV-specific functionality
 */
class CSVBufferedWriter(
    outputFile: File,
    private val headers: List<String>,
    bufferSize: Int = 8192,
    flushIntervalMs: Long = 1000L
) : BufferedDataWriter(outputFile, bufferSize, flushIntervalMs) {

    companion object {
        private const val TAG = "CSVBufferedWriter"
    }

    private val headerWritten = AtomicBoolean(false)

    /**
     * Start the writer and automatically write headers
     */
    suspend fun startWithHeaders(): Boolean {
        val started = start()
        if (started && !headerWritten.get()) {
            writeHeaders()
        }
        return started
    }

    /**
     * Write CSV headers
     */
    private suspend fun writeHeaders() {
        if (headerWritten.compareAndSet(false, true)) {
            val headerLine = headers.joinToString(",")
            writeLine(headerLine)
            Log.d(TAG, "CSV headers written: $headerLine")
        }
    }

    /**
     * Write a CSV row from a list of values
     */
    fun writeRow(values: List<Any>): Boolean {
        val csvLine = values.joinToString(",") { value ->
            when (value) {
                is String -> escapeCSVValue(value)
                else -> value.toString()
            }
        }
        return writeLine(csvLine)
    }

    /**
     * Escape CSV values that contain special characters
     */
    private fun escapeCSVValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Get CSV-specific write statistics
     */
    fun getCSVStats(): CSVWriteStats {
        val stats = getWriteStats()
        return CSVWriteStats(
            baseStats = stats,
            headerWritten = headerWritten.get(),
            columnCount = headers.size,
            headers = headers
        )
    }
}

/**
 * CSV-specific write statistics
 */
data class CSVWriteStats(
    val baseStats: WriteStats,
    val headerWritten: Boolean,
    val columnCount: Int,
    val headers: List<String>
) {
    val rowsWritten: Long
        get() = if (headerWritten) baseStats.linesWritten - 1 else baseStats.linesWritten

    val averageRowSize: Double
        get() = if (rowsWritten > 0) baseStats.bytesWritten.toDouble() / rowsWritten else 0.0
}
