package mpdc4gsr.core.data.utils

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
            return@withContext true
        }
            outputFile.parentFile?.mkdirs()
            writer = BufferedWriter(FileWriter(outputFile, true), bufferSize)
            isRunning.set(true)
            writerJob = writerScope.launch {
                runWriterLoop()
            }
            flushJob = writerScope.launch {
                runFlushLoop()
            }
            true
            cleanup()
            false
        }
    }

    fun writeLine(line: String): Boolean {
        if (!isRunning.get()) {
            return false
        }
        val success = writeQueue.offer(line)
        if (!success) {
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
                break
            }
        }
        return written
    }

    suspend fun flush() = withContext(Dispatchers.IO) {
            writer?.flush()
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        if (!isRunning.get()) {
            return@withContext
        }
        isRunning.set(false)
            writerJob?.cancel()
            flushJob?.cancel()
            writerJob?.join()
            drainQueue()
            writer?.flush()
            writer?.close()
            writer = null
            val stats = getWriteStats()
                TAG,
                "Writer stopped for ${outputFile.name}: ${stats.linesWritten} lines, ${stats.bytesWritten} bytes"
            )
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
        while (isRunning.get()) {
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
                break
                if (e is IOException) {
                    break
                }
            }
        }
    }

    private suspend fun runFlushLoop() {
        while (isRunning.get()) {
                delay(flushIntervalMs)
                if (isRunning.get()) {
                    writer?.flush()
                }
                if (e !is CancellationException) {
                }
            }
        }
    }

    private fun drainQueue() {
            val remainingLines = mutableListOf<String>()
            writeQueue.drainTo(remainingLines)
            if (remainingLines.isNotEmpty()) {
                writer?.let { w ->
                    for (line in remainingLines) {
                        w.write(line)
                        w.newLine()
                        bytesWritten.addAndGet(line.length.toLong() + 1)
                        linesWritten.incrementAndGet()
                    }
                }
            }
        }
    }

    private fun cleanup() {
            isRunning.set(false)
            writerJob?.cancel()
            flushJob?.cancel()
            writer?.close()
            writer = null
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
