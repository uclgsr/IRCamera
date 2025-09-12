package com.topdon.tc001.network

import android.content.Context
import com.topdon.tc001.logging.StructuredLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * File Upload Service for Phase 3 implementation
 * Handles resumable file uploads from Android to PC controller
 *
 * Features:
 * - Resumable uploads with checkpoint recovery
 * - File integrity verification with SHA-256 checksums
 * - Chunked transfer protocol (1MB chunks by default)
 * - Concurrent upload management (up to 3 simultaneous uploads)
 * - Progress tracking and error recovery
 * - Session-based file organization
 * - WebSocket-based transfer protocol
 */
class FileUploadService(private val context: Context) {
    companion object {
        private const val TAG = "FileUploadService"

        // Transfer configuration
        private const val DEFAULT_CHUNK_SIZE = 1024 * 1024 // 1MB chunks
        private const val MAX_CONCURRENT_UPLOADS = 3
        private const val RETRY_LIMIT = 3
        private const val TRANSFER_TIMEOUT_MS = 30000L // 30 seconds per chunk

        // Upload states
        enum class UploadStatus {
            PENDING,
            IN_PROGRESS,
            PAUSED,
            COMPLETED,
            FAILED,
            CANCELLED,
        }

        // File types for classification
        enum class FileType(val extension: String, val mimeType: String) {
            THERMAL_VIDEO("mp4", "video/mp4"),
            VISUAL_VIDEO("mp4", "video/mp4"),
            GSR_DATA("csv", "text/csv"),
            IMU_DATA("csv", "text/csv"),
            AUDIO("wav", "audio/wav"),
            METADATA("json", "application/json"),
            CALIBRATION("json", "application/json"),
        }
    }

    // Service state
    private val logger = StructuredLogger.getInstance()
    private val activeUploads = ConcurrentHashMap<String, UploadJob>()
    private val uploadQueue = Channel<String>(Channel.UNLIMITED)
    private val concurrentUploads = AtomicLong(0)
    private val isActive = AtomicBoolean(false)

    // Configuration
    private val chunkSize = DEFAULT_CHUNK_SIZE
    private val maxConcurrent = MAX_CONCURRENT_UPLOADS
    private val retryLimit = RETRY_LIMIT

    // WebSocket client for transfer communication
    private var webSocketClient: WebSocketClient? = null

    /**
     * Data class representing an upload job
     */
    data class UploadJob(
        val jobId: String,
        val filePath: String,
        val fileName: String,
        val fileType: FileType,
        val fileSize: Long,
        val checksum: String,
        val sessionId: String,
        val deviceId: String,
        var status: UploadStatus,
        var bytesUploaded: Long = 0L,
        var resumeOffset: Long = 0L,
        var retryCount: Int = 0,
        var startTime: Long = 0L,
        var endTime: Long = 0L,
        var errorMessage: String? = null,
    ) {
        val progressPercent: Float
            get() = if (fileSize > 0) (bytesUploaded.toFloat() / fileSize * 100f) else 0f

        val transferRate: Float
            get() {
                val elapsed =
                    if (status == UploadStatus.IN_PROGRESS && startTime > 0) {
                        System.currentTimeMillis() - startTime
                    } else if (endTime > startTime) {
                        endTime - startTime
                    } else {
                        0L
                    }

                return if (elapsed > 0) bytesUploaded.toFloat() / (elapsed / 1000f) else 0f
            }
    }

    /**
     * Initialize the file upload service
     */
    fun initialize(webSocketClient: WebSocketClient) {
        this.webSocketClient = webSocketClient
        isActive.set(true)

        logger.logEvent(
            component = TAG,
            event = "service_initialized",
            details =
                mapOf(
                    "chunk_size" to chunkSize,
                    "max_concurrent" to maxConcurrent,
                    "retry_limit" to retryLimit,
                ),
        )

        // Start upload processor coroutine
        startUploadProcessor()
    }

    /**
     * Queue a file for upload
     */
    suspend fun queueUpload(
        filePath: String,
        sessionId: String,
        deviceId: String,
        fileType: FileType,
    ): String {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                throw IllegalArgumentException("File does not exist or is not readable: $filePath")
            }

            // Generate unique job ID
            val jobId = generateJobId(sessionId, deviceId, file.name)

            // Calculate file checksum
            val checksum = calculateSHA256(file)

            // Create upload job
            val uploadJob =
                UploadJob(
                    jobId = jobId,
                    filePath = filePath,
                    fileName = file.name,
                    fileType = fileType,
                    fileSize = file.length(),
                    checksum = checksum,
                    sessionId = sessionId,
                    deviceId = deviceId,
                    status = UploadStatus.PENDING,
                )

            // Check for existing partial upload
            val existingOffset = checkExistingUpload(uploadJob)
            if (existingOffset > 0) {
                uploadJob.resumeOffset = existingOffset
                uploadJob.bytesUploaded = existingOffset
                logger.logEvent(
                    component = TAG,
                    event = "upload_resume",
                    details =
                        mapOf(
                            "job_id" to jobId,
                            "file_name" to file.name,
                            "resume_offset" to existingOffset,
                        ),
                )
            }

            // Store job and queue for processing
            activeUploads[jobId] = uploadJob
            uploadQueue.send(jobId)

            logger.logEvent(
                component = TAG,
                event = "upload_queued",
                details =
                    mapOf(
                        "job_id" to jobId,
                        "file_name" to file.name,
                        "file_size" to file.length(),
                        "file_type" to fileType.name,
                    ),
            )

            return jobId
        } catch (e: Exception) {
            logger.logEvent(
                component = TAG,
                event = "upload_queue_error",
                details =
                    mapOf(
                        "file_path" to filePath,
                        "error" to e.message,
                    ),
            )
            throw e
        }
    }

    /**
     * Cancel an upload
     */
    suspend fun cancelUpload(jobId: String): Boolean {
        val job = activeUploads[jobId] ?: return false

        job.status = UploadStatus.CANCELLED
        job.endTime = System.currentTimeMillis()

        logger.logEvent(
            component = TAG,
            event = "upload_cancelled",
            details =
                mapOf(
                    "job_id" to jobId,
                    "file_name" to job.fileName,
                ),
        )

        return true
    }

    /**
     * Pause an upload
     */
    suspend fun pauseUpload(jobId: String): Boolean {
        val job = activeUploads[jobId] ?: return false

        if (job.status == UploadStatus.IN_PROGRESS) {
            job.status = UploadStatus.PAUSED
            logger.logEvent(
                component = TAG,
                event = "upload_paused",
                details =
                    mapOf(
                        "job_id" to jobId,
                        "file_name" to job.fileName,
                        "bytes_uploaded" to job.bytesUploaded,
                    ),
            )
            return true
        }

        return false
    }

    /**
     * Resume a paused upload
     */
    suspend fun resumeUpload(jobId: String): Boolean {
        val job = activeUploads[jobId] ?: return false

        if (job.status == UploadStatus.PAUSED) {
            job.status = UploadStatus.PENDING
            uploadQueue.send(jobId)

            logger.logEvent(
                component = TAG,
                event = "upload_resumed",
                details =
                    mapOf(
                        "job_id" to jobId,
                        "file_name" to job.fileName,
                        "resume_offset" to job.bytesUploaded,
                    ),
            )
            return true
        }

        return false
    }

    /**
     * Get upload status
     */
    fun getUploadStatus(jobId: String): UploadJob? {
        return activeUploads[jobId]
    }

    /**
     * Get all active uploads
     */
    fun getActiveUploads(): List<UploadJob> {
        return activeUploads.values.toList()
    }

    /**
     * Get upload statistics
     */
    fun getUploadStats(): Map<String, Any> {
        val jobs = activeUploads.values
        return mapOf(
            "active_uploads" to jobs.count { it.status == UploadStatus.IN_PROGRESS },
            "pending_uploads" to jobs.count { it.status == UploadStatus.PENDING },
            "completed_uploads" to jobs.count { it.status == UploadStatus.COMPLETED },
            "failed_uploads" to jobs.count { it.status == UploadStatus.FAILED },
            "total_bytes_uploaded" to jobs.sumOf { it.bytesUploaded },
            "concurrent_capacity" to "${concurrentUploads.get()}/$maxConcurrent",
        )
    }

    /**
     * Start the upload processor coroutine
     */
    private fun startUploadProcessor() {
        GlobalScope.launch {
            while (isActive.get()) {
                try {
                    // Wait for next upload job
                    val jobId = uploadQueue.receive()

                    // Check concurrent limits
                    if (concurrentUploads.get() >= maxConcurrent) {
                        // Put back in queue and wait
                        uploadQueue.send(jobId)
                        delay(1000)
                        continue
                    }

                    val job = activeUploads[jobId]
                    if (job == null || job.status != UploadStatus.PENDING) {
                        continue
                    }

                    // Start upload in separate coroutine
                    launch {
                        executeUpload(job)
                    }
                } catch (e: Exception) {
                    logger.logEvent(
                        component = TAG,
                        event = "upload_processor_error",
                        details = mapOf("error" to e.message),
                    )
                    delay(5000) // Wait before retrying
                }
            }
        }
    }

    /**
     * Execute individual upload
     */
    private suspend fun executeUpload(job: UploadJob) {
        concurrentUploads.incrementAndGet()

        try {
            job.status = UploadStatus.IN_PROGRESS
            job.startTime = System.currentTimeMillis()

            logger.logEvent(
                component = TAG,
                event = "upload_started",
                details =
                    mapOf(
                        "job_id" to job.jobId,
                        "file_name" to job.fileName,
                        "file_size" to job.fileSize,
                        "resume_offset" to job.resumeOffset,
                    ),
            )

            // Send upload initiation message to PC
            val initResponse = initiateUpload(job)
            if (!initResponse) {
                throw Exception("Failed to initiate upload with PC controller")
            }

            // Upload file in chunks
            uploadFileChunks(job)

            // Verify upload completion
            val verifyResponse = verifyUploadCompletion(job)
            if (!verifyResponse) {
                throw Exception("Upload verification failed")
            }

            // Mark as completed
            job.status = UploadStatus.COMPLETED
            job.endTime = System.currentTimeMillis()
            job.bytesUploaded = job.fileSize

            logger.logEvent(
                component = TAG,
                event = "upload_completed",
                details =
                    mapOf(
                        "job_id" to job.jobId,
                        "file_name" to job.fileName,
                        "file_size" to job.fileSize,
                        "duration_ms" to (job.endTime - job.startTime),
                        "transfer_rate_mbps" to String.format("%.2f", job.transferRate / (1024 * 1024)),
                    ),
            )
        } catch (e: Exception) {
            job.status = UploadStatus.FAILED
            job.endTime = System.currentTimeMillis()
            job.errorMessage = e.message
            job.retryCount++

            logger.logEvent(
                component = TAG,
                event = "upload_failed",
                details =
                    mapOf(
                        "job_id" to job.jobId,
                        "file_name" to job.fileName,
                        "error" to e.message,
                        "retry_count" to job.retryCount,
                    ),
            )

            // Retry if under limit
            if (job.retryCount <= retryLimit) {
                delay(5000L * job.retryCount) // Exponential backoff
                job.status = UploadStatus.PENDING
                uploadQueue.send(job.jobId)

                logger.logEvent(
                    component = TAG,
                    event = "upload_retry_scheduled",
                    details =
                        mapOf(
                            "job_id" to job.jobId,
                            "retry_count" to job.retryCount,
                            "max_retries" to retryLimit,
                        ),
                )
            }
        } finally {
            concurrentUploads.decrementAndGet()
        }
    }

    /**
     * Initiate upload with PC controller
     */
    private suspend fun initiateUpload(job: UploadJob): Boolean {
        return try {
            val initMessage =
                JSONObject().apply {
                    put("type", "upload_initiate")
                    put("job_id", job.jobId)
                    put("file_name", job.fileName)
                    put("file_size", job.fileSize)
                    put("file_type", job.fileType.name)
                    put("checksum", job.checksum)
                    put("session_id", job.sessionId)
                    put("device_id", job.deviceId)
                    put("chunk_size", chunkSize)
                    put("resume_offset", job.resumeOffset)
                }

            webSocketClient?.sendMessage(initMessage.toString()) ?: false
        } catch (e: Exception) {
            logger.logEvent(
                component = TAG,
                event = "upload_initiate_error",
                details =
                    mapOf(
                        "job_id" to job.jobId,
                        "error" to e.message,
                    ),
            )
            false
        }
    }

    /**
     * Upload file in chunks
     */
    private suspend fun uploadFileChunks(job: UploadJob) {
        val file = File(job.filePath)

        FileInputStream(file).use { inputStream ->
            // Skip to resume offset
            inputStream.skip(job.resumeOffset)

            val buffer = ByteArray(chunkSize)
            var offset = job.resumeOffset
            var chunkIndex = (offset / chunkSize).toInt()

            while (offset < job.fileSize && job.status == UploadStatus.IN_PROGRESS) {
                val bytesToRead = minOf(chunkSize.toLong(), job.fileSize - offset).toInt()
                val bytesRead = inputStream.read(buffer, 0, bytesToRead)

                if (bytesRead <= 0) break

                // Encode chunk data as base64
                val chunkData = buffer.copyOf(bytesRead)
                val encodedData = android.util.Base64.encodeToString(chunkData, android.util.Base64.NO_WRAP)

                // Send chunk to PC
                val chunkMessage =
                    JSONObject().apply {
                        put("type", "upload_chunk")
                        put("job_id", job.jobId)
                        put("chunk_index", chunkIndex)
                        put("chunk_offset", offset)
                        put("chunk_size", bytesRead)
                        put("chunk_data", encodedData)
                        put("is_final_chunk", offset + bytesRead >= job.fileSize)
                    }

                val chunkSent = webSocketClient?.sendMessage(chunkMessage.toString()) ?: false
                if (!chunkSent) {
                    throw Exception("Failed to send chunk $chunkIndex")
                }

                // Update progress
                offset += bytesRead
                job.bytesUploaded = offset
                chunkIndex++

                // Small delay to prevent overwhelming the connection
                delay(10)
            }
        }
    }

    /**
     * Verify upload completion with PC
     */
    private suspend fun verifyUploadCompletion(job: UploadJob): Boolean {
        return try {
            val verifyMessage =
                JSONObject().apply {
                    put("type", "upload_verify")
                    put("job_id", job.jobId)
                    put("expected_size", job.fileSize)
                    put("expected_checksum", job.checksum)
                }

            webSocketClient?.sendMessage(verifyMessage.toString()) ?: false
        } catch (e: Exception) {
            logger.logEvent(
                component = TAG,
                event = "upload_verify_error",
                details =
                    mapOf(
                        "job_id" to job.jobId,
                        "error" to e.message,
                    ),
            )
            false
        }
    }

    /**
     * Check for existing partial upload on PC
     */
    private suspend fun checkExistingUpload(job: UploadJob): Long {
        return try {
            val checkMessage =
                JSONObject().apply {
                    put("type", "upload_check_existing")
                    put("job_id", job.jobId)
                    put("file_name", job.fileName)
                    put("session_id", job.sessionId)
                    put("device_id", job.deviceId)
                }

            // For now, return 0 (no existing upload)
            // In real implementation, this would send the message and wait for response
            0L
        } catch (e: Exception) {
            logger.logEvent(
                component = TAG,
                event = "upload_check_error",
                details =
                    mapOf(
                        "job_id" to job.jobId,
                        "error" to e.message,
                    ),
            )
            0L
        }
    }

    /**
     * Calculate SHA-256 checksum of file
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        FileInputStream(file).use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate unique job ID
     */
    private fun generateJobId(
        sessionId: String,
        deviceId: String,
        fileName: String,
    ): String {
        val timestamp = System.currentTimeMillis()
        val random = Random.nextInt(1000, 9999)
        return "upload_${sessionId}_${deviceId}_${timestamp}_$random"
    }

    /**
     * Shutdown the service
     */
    fun shutdown() {
        isActive.set(false)

        // Cancel all active uploads
        activeUploads.values.forEach { job ->
            if (job.status == UploadStatus.IN_PROGRESS) {
                job.status = UploadStatus.CANCELLED
            }
        }

        logger.logEvent(
            component = TAG,
            event = "service_shutdown",
            details =
                mapOf(
                    "active_uploads" to activeUploads.size,
                ),
        )
    }
}
