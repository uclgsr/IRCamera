package com.mpdc4gsr.libunified.app.utils
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
class UnifiedDataWriterUtils(
    private val outputFile: File,
    private val bufferSize: Int = 8192,
    private val flushIntervalMs: Long = 1000L,
    private val maxQueueSize: Int = 10000
) {
    private val dataQueue = LinkedBlockingQueue<String>(maxQueueSize)
    private val isRunning = AtomicBoolean(false)
    private val bytesWritten = AtomicLong(0)
    private val linesWritten = AtomicLong(0)
    private var writerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            writerJob = scope.launch {
                startWriting()
            }
            Log.d(TAG, "BufferedDataWriter started for ${outputFile.name}")
        }
    }
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            writerJob?.cancel()
            scope.launch {
                flushAll()
            }
            Log.d(TAG, "BufferedDataWriter stopped for ${outputFile.name}")
        }
    }
    fun writeData(data: String) {
        if (isRunning.get()) {
            if (!dataQueue.offer(data)) {
                Log.w(TAG, "Data queue full, dropping data")
            }
        }
    }
    fun writeCSVRow(vararg values: Any) {
        val csvLine = values.joinToString(",") { value ->
            when (value) {
                is String -> "\"${value.replace("\"", "\"\"")}\""
                else -> value.toString()
            }
        }
        writeData(csvLine)
    }
    fun writeCSVHeader(vararg headers: String) {
        writeCSVRow(*headers)
    }
    data class WriterStats(
        val bytesWritten: Long,
        val linesWritten: Long,
        val queueSize: Int,
        val isRunning: Boolean
    )
    fun getStats(): WriterStats {
        return WriterStats(
            bytesWritten = bytesWritten.get(),
            linesWritten = linesWritten.get(),
            queueSize = dataQueue.size,
            isRunning = isRunning.get()
        )
    }
    private suspend fun startWriting() {
        var bufferedWriter: BufferedWriter? = null
        try {
            outputFile.parentFile?.mkdirs()
            bufferedWriter = BufferedWriter(FileWriter(outputFile, true), bufferSize)
            var lastFlushTime = System.currentTimeMillis()
            val batch = mutableListOf<String>()
            while (isRunning.get() || dataQueue.isNotEmpty()) {
                // Collect batch of data
                batch.clear()
                val startTime = System.currentTimeMillis()
                // Collect data for up to flush interval or until batch is full
                while (batch.size < 1000 && (System.currentTimeMillis() - startTime) < flushIntervalMs) {
                    val data = dataQueue.poll()
                    if (data != null) {
                        batch.add(data)
                    } else {
                        delay(10) // Small delay to prevent busy waiting
                        break
                    }
                }
                // Write batch
                if (batch.isNotEmpty()) {
                    writeBatch(bufferedWriter, batch)
                }
                // Flush periodically
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFlushTime >= flushIntervalMs) {
                    bufferedWriter.flush()
                    lastFlushTime = currentTime
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Writer cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error in writer", e)
        } finally {
            try {
                bufferedWriter?.flush()
                bufferedWriter?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing writer", e)
            }
        }
    }
    private fun writeBatch(writer: BufferedWriter, batch: List<String>) {
        for (data in batch) {
            writer.write(data)
            writer.newLine()
            bytesWritten.addAndGet(data.length.toLong() + 1) // +1 for newline
            linesWritten.incrementAndGet()
        }
    }
    private suspend fun flushAll() = withContext(Dispatchers.IO) {
        try {
            if (outputFile.exists()) {
                BufferedWriter(FileWriter(outputFile, true)).use { writer ->
                    val remainingData = mutableListOf<String>()
                    while (dataQueue.isNotEmpty()) {
                        dataQueue.poll()?.let { remainingData.add(it) }
                    }
                    writeBatch(writer, remainingData)
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing remaining data", e)
        }
    }
    // Static utility methods for simple file operations
    companion object {
        private const val TAG = "UnifiedDataWriter"
        fun writeToFile(file: File, data: String, append: Boolean = false) {
            try {
                file.parentFile?.mkdirs()
                file.writeText(data, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to file: ${file.name}", e)
            }
        }
        fun writeCSVToFile(file: File, headers: Array<String>, rows: List<Array<Any>>) {
            try {
                file.parentFile?.mkdirs()
                BufferedWriter(FileWriter(file)).use { writer ->
                    // Write header
                    writer.write(headers.joinToString(",") { "\"$it\"" })
                    writer.newLine()
                    // Write rows
                    for (row in rows) {
                        val csvLine = row.joinToString(",") { value ->
                            when (value) {
                                is String -> "\"${value.replace("\"", "\"\"")}\""
                                else -> value.toString()
                            }
                        }
                        writer.write(csvLine)
                        writer.newLine()
                    }
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing CSV to file: ${file.name}", e)
            }
        }
        fun createWriter(
            outputFile: File,
            bufferSize: Int = 8192,
            flushIntervalMs: Long = 1000L,
            maxQueueSize: Int = 10000
        ): UnifiedDataWriterUtils {
            return UnifiedDataWriterUtils(outputFile, bufferSize, flushIntervalMs, maxQueueSize)
        }
    }
}