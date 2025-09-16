package com.topdon.tc001.util

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance buffered data writer for sensor data
 * Provides efficient writing with periodic flushing and proper cleanup
 */
class BufferedDataWriter(
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
    
    /**
     * Start the buffered writer
     */
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (isRunning.get()) {
            Log.w(TAG, "Writer already running for ${outputFile.name}")
            return@withContext true
        }
        
        try {
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            // Create writer with buffer
            writer = BufferedWriter(FileWriter(outputFile, true), bufferSize)
            isRunning.set(true)
            
            // Start writer coroutine
            writerJob = writerScope.launch {
                runWriterLoop()
            }
            
            // Start periodic flush coroutine
            flushJob = writerScope.launch {
                runFlushLoop()
            }
            
            Log.i(TAG, "Started buffered writer for ${outputFile.absolutePath}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start writer for ${outputFile.name}", e)
            cleanup()
            false
        }
    }
    
    /**
     * Write a line to the buffer (non-blocking)
     */
    fun writeLine(line: String): Boolean {
        if (!isRunning.get()) {
            Log.w(TAG, "Writer not running, cannot write line")
            return false
        }
        
        val success = writeQueue.offer(line)
        if (!success) {
            Log.w(TAG, "Write queue full, dropping line for ${outputFile.name}")
        }
        
        return success
    }
    
    /**
     * Write multiple lines efficiently
     */
    fun writeLines(lines: List<String>): Int {
        if (!isRunning.get()) {
            return 0
        }
        
        var written = 0
        for (line in lines) {
            if (writeQueue.offer(line)) {
                written++
            } else {
                Log.w(TAG, "Write queue full, stopping batch write")
                break
            }
        }
        
        return written
    }
    
    /**
     * Force flush all pending data
     */
    suspend fun flush() = withContext(Dispatchers.IO) {
        try {
            writer?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush writer for ${outputFile.name}", e)
        }
    }
    
    /**
     * Stop the writer and cleanup resources
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        if (!isRunning.get()) {
            return@withContext
        }
        
        Log.i(TAG, "Stopping buffered writer for ${outputFile.name}")
        isRunning.set(false)
        
        try {
            // Cancel jobs
            writerJob?.cancel()
            flushJob?.cancel()
            
            // Wait for any remaining writes
            writerJob?.join()
            
            // Write any remaining queued data
            drainQueue()
            
            // Final flush and close
            writer?.flush()
            writer?.close()
            writer = null
            
            val stats = getWriteStats()
            Log.i(TAG, "Writer stopped for ${outputFile.name}: ${stats.linesWritten} lines, ${stats.bytesWritten} bytes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping writer for ${outputFile.name}", e)
        }
    }
    
    /**
     * Get current write statistics
     */
    fun getWriteStats(): WriteStats {
        return WriteStats(
            fileName = outputFile.name,
            bytesWritten = bytesWritten.get(),
            linesWritten = linesWritten.get(),
            queueSize = writeQueue.size,
            isRunning = isRunning.get()
        )
    }
    
    /**
     * Main writer loop - processes queued writes
     */
    private suspend fun runWriterLoop() {
        Log.d(TAG, "Starting writer loop for ${outputFile.name}")
        
        while (isRunning.get()) {
            try {
                val line = withContext(Dispatchers.IO) {
                    // Use blocking take with timeout to allow cancellation
                    runInterruptible {
                        writeQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                }
                
                if (line != null) {
                    writer?.let { w ->
                        w.write(line)
                        w.newLine()
                        
                        bytesWritten.addAndGet(line.length.toLong() + 1) // +1 for newline
                        linesWritten.incrementAndGet()
                    }
                }
                
            } catch (e: InterruptedException) {
                Log.d(TAG, "Writer loop interrupted for ${outputFile.name}")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in writer loop for ${outputFile.name}", e)
                if (e is IOException) {
                    // File system error - stop writing
                    break
                }
            }
        }
        
        Log.d(TAG, "Writer loop ended for ${outputFile.name}")
    }
    
    /**
     * Periodic flush loop
     */
    private suspend fun runFlushLoop() {
        while (isRunning.get()) {
            try {
                delay(flushIntervalMs)
                
                if (isRunning.get()) {
                    writer?.flush()
                }
                
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Error in flush loop for ${outputFile.name}", e)
                }
            }
        }
    }
    
    /**
     * Drain remaining items from queue
     */
    private fun drainQueue() {
        try {
            val remainingLines = mutableListOf<String>()
            writeQueue.drainTo(remainingLines)
            
            if (remainingLines.isNotEmpty()) {
                Log.i(TAG, "Writing ${remainingLines.size} remaining lines for ${outputFile.name}")
                
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
            Log.e(TAG, "Error draining queue for ${outputFile.name}", e)
        }
    }
    
    /**
     * Cleanup resources
     */
    private fun cleanup() {
        try {
            isRunning.set(false)
            writerJob?.cancel()
            flushJob?.cancel()
            writer?.close()
            writer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup for ${outputFile.name}", e)
        }
    }
}

/**
 * Statistics for data writing operations
 */
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

/**
 * CSV-specific buffered writer with header support
 */
class CSVBufferedWriter(
    outputFile: File,
    private val headers: List<String>,
    bufferSize: Int = 8192,
    flushIntervalMs: Long = 1000L
) : BufferedDataWriter(outputFile, bufferSize, flushIntervalMs) {
    
    private val headerWritten = AtomicBoolean(false)
    
    suspend fun startWithHeaders(): Boolean {
        val started = start()
        if (started && !headerWritten.getAndSet(true)) {
            val headerLine = headers.joinToString(",")
            writeLine(headerLine)
        }
        return started
    }
    
    fun writeRow(values: List<Any>): Boolean {
        val csvLine = values.joinToString(",") { value ->
            when (value) {
                is String -> "\"${value.replace("\"", "\"\"")}\""
                else -> value.toString()
            }
        }
        return writeLine(csvLine)
    }
}
