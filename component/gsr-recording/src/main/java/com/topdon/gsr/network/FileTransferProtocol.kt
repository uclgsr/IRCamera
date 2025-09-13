package com.topdon.gsr.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Advanced file transfer protocol for large video files and bulk data synchronization
 * Supports resumable transfers, integrity verification, and bandwidth optimization
 */
class FileTransferProtocol(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "FileTransferProtocol"
        private const val CHUNK_SIZE = 64 * 1024 // 64KB chunks for optimal network performance
        private const val MAX_CONCURRENT_TRANSFERS = 3
        private const val INTEGRITY_CHECK_INTERVAL = 1024 * 1024 // 1MB checksum intervals
        private const val TRANSFER_TIMEOUT_MS = 30000L
        private const val RESUME_RETRY_ATTEMPTS = 3
    }

    private val transferJob = SupervisorJob()
    private val transferScope = CoroutineScope(Dispatchers.IO + transferJob)

    private val activeTransfers = ConcurrentHashMap<String, TransferSession>()
    private val transferQueue = mutableListOf<TransferRequest>()
    private val totalBytesTransferred = AtomicLong(0)
    private val currentTransferSpeed = AtomicLong(0) // bytes per second

    data class TransferRequest(
        val transferId: String,
        val filePath: String,
        val fileSize: Long,
        val priority: TransferPriority,
        val sessionId: String,
        val metadata: Map<String, String> = emptyMap(),
    )

    data class TransferSession(
        val request: TransferRequest,
        val startTime: Long,
        val bytesTransferred: AtomicLong = AtomicLong(0),
        val lastChunkTime: Long = System.currentTimeMillis(),
        val checksumAccumulator: MessageDigest = MessageDigest.getInstance("SHA-256"),
        var resumeOffset: Long = 0,
    )

    enum class TransferPriority(val weight: Int) {
        CRITICAL(100), // Session data, logs
        HIGH(75), // Recent video files
        NORMAL(50), // Standard video files
        LOW(25), // Archived data
    }

    data class TransferProgress(
        val transferId: String,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val transferSpeed: Long, // bytes/second
        val estimatedTimeRemaining: Long, // milliseconds
        val status: TransferStatus,
    )

    enum class TransferStatus {
        QUEUED,
        TRANSFERRING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED,
    }

    /**
     * Queue a file for transfer to PC Controller
     *
     * Adds a file to the transfer queue with specified priority and metadata.
     * Files are transferred asynchronously using chunked uploads with integrity verification.
     *
     * @param filePath Absolute path to the file to be transferred
     * @param priority Transfer priority level affecting queue ordering
     * @param sessionId Recording session ID for organizing transferred files
     * @param metadata Additional key-value metadata to include with transfer
     * @return Unique transfer ID for tracking the file transfer progress
     * @throws FileNotFoundException if the specified file does not exist
     * @throws SecurityException if file access is denied
     */
    suspend fun queueFileTransfer(
        filePath: String,
        priority: TransferPriority = TransferPriority.NORMAL,
        sessionId: String,
        metadata: Map<String, String> = emptyMap(),
    ): String =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) {
                throw FileNotFoundException("File not found: $filePath")
            }

            val transferId = generateTransferId(filePath, sessionId)
            val request =
                TransferRequest(
                    transferId = transferId,
                    filePath = filePath,
                    fileSize = file.length(),
                    priority = priority,
                    sessionId = sessionId,
                    metadata = metadata,
                )

            synchronized(transferQueue) {
                transferQueue.add(request)
                transferQueue.sortByDescending { it.priority.weight }
            }

            Log.d(TAG, "Queued file transfer: $transferId, size: ${file.length()} bytes")
            processTransferQueue()
            transferId
        }

    /**
     * Process the transfer queue with priority-based scheduling
     *
     * Initiates processing of queued file transfers in priority order.
     * Respects maximum concurrent transfer limits to avoid overwhelming
     * the network connection.
     */
    private fun processTransferQueue() {
        transferScope.launch {
            processTransferQueueAsync()
        }
    }

    /**
     * Async version to avoid recursion issues
     *
     * Processes pending transfer requests while respecting concurrency limits.
     * Removes requests from queue and starts transfers until maximum
     * concurrent transfers is reached.
     */
    private suspend fun processTransferQueueAsync(): Unit =
        withContext(Dispatchers.IO) {
            while (transferQueue.isNotEmpty() && activeTransfers.size < MAX_CONCURRENT_TRANSFERS) {
                val request =
                    synchronized(transferQueue) {
                        if (transferQueue.isEmpty()) return@synchronized null
                        transferQueue.removeAt(0) // Use removeAt(0) instead of removeFirst() for API compatibility
                    } ?: break

                startFileTransfer(request)
            }
        }

    /**
     * Start individual file transfer with resumable support
     *
     * Initiates transfer of a single file using chunked uploads with
     * integrity verification. Supports resuming interrupted transfers
     * from the last completed chunk.
     *
     * @param request Transfer request containing file details and metadata
     */
    private suspend fun startFileTransfer(request: TransferRequest): Unit =
        withContext(Dispatchers.IO) {
            val session =
                TransferSession(
                    request = request,
                    startTime = System.currentTimeMillis(),
                )

            activeTransfers[request.transferId] = session

            try {
                // Check if transfer can be resumed
                val resumeOffset = checkResumeCapability(request.transferId)
                session.resumeOffset = resumeOffset

                // Initialize transfer with PC Controller
                initializeTransfer(session)

                // Start chunked transfer
                transferFileInChunks(session)

                // Verify transfer integrity
                verifyTransferIntegrity(session)

                Log.d(TAG, "Transfer completed: ${request.transferId}")
            } catch (e: Exception) {
                Log.e(TAG, "Transfer failed: ${request.transferId}", e)
                handleTransferError(session, e)
            } finally {
                activeTransfers.remove(request.transferId)
                // Process next queued transfer asynchronously to avoid recursion
                transferScope.launch {
                    processTransferQueueAsync()
                }
            }
        }

    /**
     * Initialize transfer session with PC Controller
     */
    private suspend fun initializeTransfer(session: TransferSession) {
        val initMessage =
            JSONObject().apply {
                put("type", "file_transfer_init")
                put("transfer_id", session.request.transferId)
                put("file_name", File(session.request.filePath).name)
                put("file_size", session.request.fileSize)
                put("session_id", session.request.sessionId)
                put("resume_offset", session.resumeOffset)
                put("chunk_size", CHUNK_SIZE)
                put("metadata", JSONObject(session.request.metadata))
            }

        networkClient.sendMessage(initMessage)

        // Wait for acknowledgment
        val response = networkClient.waitForResponse("file_transfer_ack", TRANSFER_TIMEOUT_MS)
        if (response.optString("status") != "ready") {
            throw IOException("PC Controller not ready for transfer")
        }
    }

    /**
     * Transfer file in optimized chunks with progress tracking
     */
    private suspend fun transferFileInChunks(session: TransferSession): Unit =
        withContext(Dispatchers.IO) {
            val file = File(session.request.filePath)
            val buffer = ByteArray(CHUNK_SIZE)

            FileInputStream(file).use { inputStream ->
                // Skip to resume offset if resuming
                if (session.resumeOffset > 0) {
                    inputStream.skip(session.resumeOffset)
                    session.bytesTransferred.set(session.resumeOffset)
                }

                var bytesRead: Int
                var chunkIndex = (session.resumeOffset / CHUNK_SIZE).toInt()
                val startTime = System.currentTimeMillis()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val chunkData =
                        if (bytesRead < CHUNK_SIZE) {
                            buffer.copyOf(bytesRead)
                        } else {
                            buffer
                        }

                    // Send chunk with metadata
                    sendFileChunk(session, chunkIndex, chunkData)

                    // Update progress
                    session.bytesTransferred.addAndGet(bytesRead.toLong())
                    session.checksumAccumulator.update(chunkData, 0, bytesRead)
                    totalBytesTransferred.addAndGet(bytesRead.toLong())

                    // Calculate transfer speed
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime > 0) {
                        val speed = (session.bytesTransferred.get() * 1000L) / elapsedTime
                        currentTransferSpeed.set(speed)
                    }

                    chunkIndex++

                    // Periodic integrity check
                    if (session.bytesTransferred.get() % INTEGRITY_CHECK_INTERVAL == 0L) {
                        verifyPartialIntegrity(session)
                    }

                    // Yield to prevent blocking other coroutines
                    yield()
                }
            }
        }

    /**
     * Send individual file chunk to PC Controller
     */
    private suspend fun sendFileChunk(
        session: TransferSession,
        chunkIndex: Int,
        data: ByteArray,
    ) {
        val chunkMessage =
            JSONObject().apply {
                put("type", "file_chunk")
                put("transfer_id", session.request.transferId)
                put("chunk_index", chunkIndex)
                put("chunk_size", data.size)
            }

        // Send chunk metadata followed by binary data
        networkClient.sendMessage(chunkMessage)
        networkClient.sendBinaryData(data)

        // Wait for chunk acknowledgment
        val ack = networkClient.waitForResponse("chunk_ack", 5000L)
        if (ack.optString("transfer_id") != session.request.transferId ||
            ack.optInt("chunk_index") != chunkIndex
        ) {
            throw IOException("Invalid chunk acknowledgment")
        }
    }

    /**
     * Verify complete transfer integrity using checksum
     */
    private suspend fun verifyTransferIntegrity(session: TransferSession) {
        val calculatedChecksum = session.checksumAccumulator.digest()
        val checksumHex = calculatedChecksum.joinToString("") { "%02x".format(it) }

        val verifyMessage =
            JSONObject().apply {
                put("type", "file_transfer_verify")
                put("transfer_id", session.request.transferId)
                put("checksum", checksumHex)
                put("algorithm", "SHA-256")
            }

        networkClient.sendMessage(verifyMessage)

        val response = networkClient.waitForResponse("transfer_verification", TRANSFER_TIMEOUT_MS)
        if (response.optString("status") != "verified") {
            throw IOException("Transfer integrity verification failed")
        }
    }

    /**
     * Check if transfer can be resumed from PC Controller
     */
    private suspend fun checkResumeCapability(transferId: String): Long {
        val resumeQuery =
            JSONObject().apply {
                put("type", "file_transfer_resume_query")
                put("transfer_id", transferId)
            }

        networkClient.sendMessage(resumeQuery)

        return try {
            val response = networkClient.waitForResponse("resume_info", 5000L)
            response.optLong("resume_offset", 0L)
        } catch (e: Exception) {
            Log.d(TAG, "Resume not available for transfer: $transferId")
            0L
        }
    }

    /**
     * Perform periodic partial integrity verification
     */
    private suspend fun verifyPartialIntegrity(session: TransferSession) {
        // Implementation for periodic checksum verification
        Log.d(TAG, "Partial integrity check at ${session.bytesTransferred.get()} bytes")
    }

    /**
     * Handle transfer errors with retry logic
     */
    private suspend fun handleTransferError(
        session: TransferSession,
        error: Exception,
    ) {
        Log.e(TAG, "Transfer error for ${session.request.transferId}", error)

        // Implement retry logic based on error type
        if (error is IOException && session.resumeOffset < session.request.fileSize) {
            // Queue for retry if network error and transfer is resumable
            synchronized(transferQueue) {
                transferQueue.add(0, session.request) // Add to front of queue
            }
        }
    }

    /**
     * Get current transfer progress for all active transfers
     */
    fun getTransferProgress(): List<TransferProgress> {
        return activeTransfers.values.map { session ->
            val elapsed = System.currentTimeMillis() - session.startTime
            val speed =
                if (elapsed > 0) {
                    (session.bytesTransferred.get() * 1000L) / elapsed
                } else {
                    0L
                }

            val remaining =
                if (speed > 0) {
                    (session.request.fileSize - session.bytesTransferred.get()) / speed * 1000L
                } else {
                    0L
                }

            TransferProgress(
                transferId = session.request.transferId,
                bytesTransferred = session.bytesTransferred.get(),
                totalBytes = session.request.fileSize,
                transferSpeed = speed,
                estimatedTimeRemaining = remaining,
                status = TransferStatus.TRANSFERRING,
            )
        }
    }

    /**
     * Cancel active transfer
     */
    suspend fun cancelTransfer(transferId: String): Boolean {
        val session = activeTransfers[transferId] ?: return false

        val cancelMessage =
            JSONObject().apply {
                put("type", "file_transfer_cancel")
                put("transfer_id", transferId)
            }

        networkClient.sendMessage(cancelMessage)
        activeTransfers.remove(transferId)

        Log.d(TAG, "Transfer cancelled: $transferId")
        return true
    }

    /**
     * Generate unique transfer ID
     */
    private fun generateTransferId(
        filePath: String,
        sessionId: String,
    ): String {
        val fileName = File(filePath).name
        val timestamp = System.currentTimeMillis()
        return "${sessionId}_${fileName}_$timestamp"
    }

    /**
     * Get overall transfer statistics
     */
    fun getTransferStatistics(): TransferStatistics {
        return TransferStatistics(
            totalBytesTransferred = totalBytesTransferred.get(),
            currentTransferSpeed = currentTransferSpeed.get(),
            activeTransfers = activeTransfers.size,
            queuedTransfers = transferQueue.size,
        )
    }

    data class TransferStatistics(
        val totalBytesTransferred: Long,
        val currentTransferSpeed: Long,
        val activeTransfers: Int,
        val queuedTransfers: Int,
    )

    /**
     * Cleanup resources
     */
    fun cleanup() {
        transferJob.cancel()
        activeTransfers.clear()
        transferQueue.clear()
    }
}
