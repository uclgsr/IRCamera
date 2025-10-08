package mpdc4gsr.core.data.utils

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

open class BufferedDataWriter(
    private val outputFile: File,
    private val bufferSize: Int = 8192,
    private val flushIntervalMs: Long = 1000L,
    private val maxQueueSize: Int = 10000
) {
    companion object {
        private const val TAG = "BufferedDataWriter"
    }

    private var writer: BufferedWriter? = null
    private val writeQueue = LinkedBlockingQueue<String>(maxQueueSize)
    private val isRunning = AtomicBoolean(false)
    private val bytesWritten = AtomicLong(0)
    private val linesWritten = AtomicLong(0)
    private var writerJob: Job? = null
    private var flushJob: Job? = null
    private val writerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (isRunning.get()) {
            AppLogger.w(TAG, "Writer already running for ${outputFile.name}")
            return@withContext true
        }
        try {
            outputFile.parentFile?.mkdirs()
            writer = BufferedWriter(FileWriter(outputFile, true), bufferSize)
            isRunning.set(true)
            writerJob = writerScope.launch {
                runWriterLoop()
            }
            flushJob = writerScope.launch {
                runFlushLoop()
            }
            AppLogger.i(TAG, "Started buffered writer for ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start writer for ${outputFile.name}", e)
            cleanup()
            false
        }
    }

    fun writeLine(line: String): Boolean {
        if (!isRunning.get()) {
            AppLogger.w(TAG, "Writer not running, cannot write line")
            return false
        }
        val success = writeQueue.offer(line)
        if (!success) {
            AppLogger.w(TAG, "Write queue full, dropping line for ${outputFile.name}")
        }
        return success
    }

    fun writeLines(lines: List<String>): Int {
        if (!isRunning.get()) {
            return 0
        }
        var written = 0
        for (line in lines) {
            if (writeQueue.offer(line)) {
                written++
            } else {
                AppLogger.w(TAG, "Write queue full, stopping batch write")
                break
            }
        }
        return written
    }

    suspend fun flush() = withContext(Dispatchers.IO) {
        try {
            writer?.flush()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to flush writer for ${outputFile.name}", e)
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        if (!isRunning.get()) {
            return@withContext
        }
        AppLogger.i(TAG, "Stopping buffered writer for ${outputFile.name}")
        isRunning.set(false)
        try {
            writerJob?.cancel()
            flushJob?.cancel()
            writerJob?.join()
            drainQueue()
            writer?.flush()
            writer?.close()
            writer = null
            val stats = getWriteStats()
            Log.i(
                TAG,
                "Writer stopped for ${outputFile.name}: ${stats.linesWritten} lines, ${stats.bytesWritten} bytes"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping writer for ${outputFile.name}", e)
        }
    }

    fun getWriteStats(): WriteStats {
        return WriteStats(
            fileName = outputFile.name,
            bytesWritten = bytesWritten.get(),
            linesWritten = linesWritten.get(),
            queueSize = writeQueue.size,
            isRunning = isRunning.get()
        )
    }

    private suspend fun runWriterLoop() {
        AppLogger.d(TAG, "Starting writer loop for ${outputFile.name}")
        while (isRunning.get()) {
            try {
                val line = withContext(Dispatchers.IO) {
                    runInterruptible {
                        writeQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                }
                if (line != null) {
                    writer?.let { w ->
                        w.write(line)
                        w.newLine()
                        bytesWritten.addAndGet(line.length.toLong() + 1)
                        linesWritten.incrementAndGet()
                    }
                }
            } catch (e: InterruptedException) {
                AppLogger.d(TAG, "Writer loop interrupted for ${outputFile.name}")
                break
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error in writer loop for ${outputFile.name}", e)
                if (e is IOException) {
                    break
                }
            }
        }
        AppLogger.d(TAG, "Writer loop ended for ${outputFile.name}")
    }

    private suspend fun runFlushLoop() {
        while (isRunning.get()) {
            try {
                delay(flushIntervalMs)
                if (isRunning.get()) {
                    writer?.flush()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    AppLogger.e(TAG, "Error in flush loop for ${outputFile.name}", e)
                }
            }
        }
    }

    private fun drainQueue() {
        try {
            val remainingLines = mutableListOf<String>()
            writeQueue.drainTo(remainingLines)
            if (remainingLines.isNotEmpty()) {
                AppLogger.i(TAG, "Writing ${remainingLines.size} remaining lines for ${outputFile.name}")
                writer?.let { w ->
                    for (line in remainingLines) {
                        w.write(line)
                        w.newLine()
                        bytesWritten.addAndGet(line.length.toLong() + 1)
                        linesWritten.incrementAndGet()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error draining queue for ${outputFile.name}", e)
        }
    }

    private fun cleanup() {
        try {
            isRunning.set(false)
            writerJob?.cancel()
            flushJob?.cancel()
            writer?.close()
            writer = null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during cleanup for ${outputFile.name}", e)
        }
    }
}

data class WriteStats(
    val fileName: String,
    val bytesWritten: Long,
    val linesWritten: Long,
    val queueSize: Int,
    val isRunning: Boolean
) {
    val avgLineSize: Double
        get() = if (linesWritten > 0) bytesWritten.toDouble() / linesWritten else 0.0
    val formattedSize: String
        get() = when {
            bytesWritten > 1024 * 1024 -> String.format("%.2f MB", bytesWritten / (1024.0 * 1024.0))
            bytesWritten > 1024 -> String.format("%.2f KB", bytesWritten / 1024.0)
            else -> "$bytesWritten bytes"
        }
}
