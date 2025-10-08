package mpdc4gsr.core.data.utils

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CSVBufferedWriter(
    outputFile: File,
    private val headers: List<String>,
    bufferSize: Int = 8192,
    flushIntervalMs: Long = 1000L
) : BufferedDataWriter(outputFile, bufferSize, flushIntervalMs) {
    companion object {
    }

    private val headerWritten = AtomicBoolean(false)
    suspend fun startWithHeaders(): Boolean {
        val started = start()
        if (started && !headerWritten.get()) {
            writeHeaders()
        }
        return started
    }

    private suspend fun writeHeaders() {
        if (headerWritten.compareAndSet(false, true)) {
            val headerLine = headers.joinToString(",")
            writeLine(headerLine)
        }
    }

    fun writeRow(values: List<Any>): Boolean {
        val csvLine = values.joinToString(",") { value ->
            when (value) {
                is String -> escapeCSVValue(value)
                else -> value.toString()
            }
        }
        return writeLine(csvLine)
    }

    private fun escapeCSVValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

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
