package mpdc4gsr.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
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
        if (isRunning.get()) {            return@withContext true
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
            }            true

        } catch (e: Exception) {            cleanup()
            false
        }
    }

    fun writeLine(line: String): Boolean {
        if (!isRunning.get()) {            return false
        }

        val success = writeQueue.offer(line)
        if (!success) {        }

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
            } else {                break
            }
        }

        return written
    }

    suspend fun flush() = withContext(Dispatchers.IO) {
        try {
            writer?.flush()
        } catch (e: Exception) {        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        if (!isRunning.get()) {
            return@withContext
        }        isRunning.set(false)

        try {
            writerJob?.cancel()
            flushJob?.cancel()

            writerJob?.join()

            drainQueue()

            writer?.flush()
            writer?.close()
            writer = null

            val stats = getWriteStats()        } catch (e: Exception) {        }
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

    private suspend fun runWriterLoop() {        while (isRunning.get()) {
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

            } catch (e: InterruptedException) {                break
            } catch (e: Exception) {                if (e is IOException) {
                    break
                }
            }
        }    }

    private suspend fun runFlushLoop() {
        while (isRunning.get()) {
            try {
                delay(flushIntervalMs)

                if (isRunning.get()) {
                    writer?.flush()
                }

            } catch (e: Exception) {
                if (e !is CancellationException) {                }
            }
        }
    }

    private fun drainQueue() {
        try {
            val remainingLines = mutableListOf<String>()
            writeQueue.drainTo(remainingLines)

            if (remainingLines.isNotEmpty()) {                writer?.let { w ->
                    for (line in remainingLines) {
                        w.write(line)
                        w.newLine()
                        bytesWritten.addAndGet(line.length.toLong() + 1)
                        linesWritten.incrementAndGet()
                    }
                }
            }
        } catch (e: Exception) {        }
    }

    private fun cleanup() {
        try {
            isRunning.set(false)
            writerJob?.cancel()
            flushJob?.cancel()
            writer?.close()
            writer = null
        } catch (e: Exception) {        }
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
