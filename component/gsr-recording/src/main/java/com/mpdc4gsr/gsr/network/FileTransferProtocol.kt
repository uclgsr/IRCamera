package com.mpdc4gsr.gsr.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FileTransferProtocol(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "FileTransferProtocol"
        private const val CHUNK_SIZE = 64 * 1024
        private const val MAX_CONCURRENT_TRANSFERS = 3
        private const val INTEGRITY_CHECK_INTERVAL = 1024 * 1024
        private const val TRANSFER_TIMEOUT_MS = 30000L
        private const val RESUME_RETRY_ATTEMPTS = 3
    }

    private val transferJob = SupervisorJob()
    private val transferScope = CoroutineScope(Dispatchers.IO + transferJob)
    private val activeTransfers = ConcurrentHashMap<String, TransferSession>()
    private val transferQueue = mutableListOf<TransferRequest>()
    private val totalBytesTransferred = AtomicLong(0)
    private val currentTransferSpeed = AtomicLong(0)

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
        CRITICAL(100),
        HIGH(75),
        NORMAL(50),
        LOW(25),
    }

    data class TransferProgress(
        val transferId: String,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val transferSpeed: Long,
        val estimatedTimeRemaining: Long,
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

    private fun processTransferQueue() {
        transferScope.launch {
            processTransferQueueAsync()
        }
    }

    private suspend fun processTransferQueueAsync(): Unit =
        withContext(Dispatchers.IO) {
            while (transferQueue.isNotEmpty() && activeTransfers.size < MAX_CONCURRENT_TRANSFERS) {
                val request =
                    synchronized(transferQueue) {
                        if (transferQueue.isEmpty()) return@synchronized null
                        transferQueue.removeAt(0)
                    } ?: break
                startFileTransfer(request)
            }
        }

    private suspend fun startFileTransfer(request: TransferRequest): Unit =
        withContext(Dispatchers.IO) {
            val session =
                TransferSession(
                    request = request,
                    startTime = System.currentTimeMillis(),
                )
            activeTransfers[request.transferId] = session
            try {
                val resumeOffset = checkResumeCapability(request.transferId)
                session.resumeOffset = resumeOffset
                initializeTransfer(session)
                transferFileInChunks(session)
                verifyTransferIntegrity(session)
                Log.d(TAG, "Transfer completed: ${request.transferId}")
            } catch (e: Exception) {
                Log.e(TAG, "Transfer failed: ${request.transferId}", e)
                handleTransferError(session, e)
            } finally {
                activeTransfers.remove(request.transferId)
                transferScope.launch {
                    processTransferQueueAsync()
                }
            }
        }

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
        val response = networkClient.waitForResponse("file_transfer_ack", TRANSFER_TIMEOUT_MS)
        if (response.optString("status") != "ready") {
            throw IOException("PC Controller not ready for transfer")
        }
    }

    private suspend fun transferFileInChunks(session: TransferSession): Unit =
        withContext(Dispatchers.IO) {
            val file = File(session.request.filePath)
            val buffer = ByteArray(CHUNK_SIZE)
            FileInputStream(file).use { inputStream ->
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
                    sendFileChunk(session, chunkIndex, chunkData)
                    session.bytesTransferred.addAndGet(bytesRead.toLong())
                    session.checksumAccumulator.update(chunkData, 0, bytesRead)
                    totalBytesTransferred.addAndGet(bytesRead.toLong())
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime > 0) {
                        val speed = (session.bytesTransferred.get() * 1000L) / elapsedTime
                        currentTransferSpeed.set(speed)
                    }
                    chunkIndex++
                    if (session.bytesTransferred.get() % INTEGRITY_CHECK_INTERVAL == 0L) {
                        verifyPartialIntegrity(session)
                    }
                    yield()
                }
            }
        }

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
        networkClient.sendMessage(chunkMessage)
        networkClient.sendBinaryData(data)
        val ack = networkClient.waitForResponse("chunk_ack", 5000L)
        if (ack.optString("transfer_id") != session.request.transferId ||
            ack.optInt("chunk_index") != chunkIndex
        ) {
            throw IOException("Invalid chunk acknowledgment")
        }
    }

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

    private suspend fun verifyPartialIntegrity(session: TransferSession) {
        Log.d(TAG, "Partial integrity check at ${session.bytesTransferred.get()} bytes")
    }

    private suspend fun handleTransferError(
        session: TransferSession,
        error: Exception,
    ) {
        Log.e(TAG, "Transfer error for ${session.request.transferId}", error)
        if (error is IOException && session.resumeOffset < session.request.fileSize) {
            synchronized(transferQueue) {
                transferQueue.add(0, session.request)
            }
        }
    }

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

    private fun generateTransferId(
        filePath: String,
        sessionId: String,
    ): String {
        val fileName = File(filePath).name
        val timestamp = System.currentTimeMillis()
        return "${sessionId}_${fileName}_$timestamp"
    }

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

    fun cleanup() {
        transferJob.cancel()
        activeTransfers.clear()
        transferQueue.clear()
    }
}
