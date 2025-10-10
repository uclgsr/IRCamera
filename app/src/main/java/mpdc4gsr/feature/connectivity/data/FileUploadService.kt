package mpdc4gsr.feature.connectivity.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.common.logging.StructuredLogger
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class FileUploadService(private val context: Context) {
    enum class UploadStatus {
        PENDING,
        IN_PROGRESS,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED,
    }


    enum class FileType(val extension: String, val mimeType: String) {
        THERMAL_VIDEO("mp4", "video/mp4"),
        VISUAL_VIDEO("mp4", "video/mp4"),
        GSR_DATA("csv", "text/csv"),
        IMU_DATA("csv", "text/csv"),
        AUDIO("wav", "audio/wav"),
        METADATA("json", "application/json"),
        CALIBRATION("json", "application/json"),
    }


    companion object {
        private const val BYTES_PER_MB = 1024 * 1024
        private const val DEFAULT_CHUNK_SIZE = BYTES_PER_MB
        private const val MAX_CONCURRENT_UPLOADS = 3
        private const val RETRY_LIMIT = 3
        private const val TRANSFER_TIMEOUT_MS = 30000L
        private const val QUEUE_RETRY_DELAY_MS = 1000L
        private const val ERROR_RETRY_DELAY_MS = 5000L
    }


    private val logger = StructuredLogger.getInstance(context)
    private val activeUploads = ConcurrentHashMap<String, UploadJob>()
    private val uploadQueue = Channel<String>(Channel.UNLIMITED)
    private val concurrentUploads = AtomicLong(0)
    private val isActive = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val chunkSize = DEFAULT_CHUNK_SIZE
    private val maxConcurrent = MAX_CONCURRENT_UPLOADS
    private val retryLimit = RETRY_LIMIT
    private var webSocketClient: WebSocketClient? = null

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


    fun initialize(webSocketClient: WebSocketClient) {
        this.webSocketClient = webSocketClient
        isActive.set(true)
        startUploadProcessor()
    }


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

            val jobId = generateJobId(sessionId, deviceId, file.name)
            val checksum = calculateSHA256(file)
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
            val existingOffset = checkExistingUpload(uploadJob)
            if (existingOffset > 0) {
                uploadJob.resumeOffset = existingOffset
                uploadJob.bytesUploaded = existingOffset
            }

            activeUploads[jobId] = uploadJob
            uploadQueue.send(jobId)
            return jobId
        } catch (e: Exception) {
            throw e
        }
    }


    suspend fun cancelUpload(jobId: String): Boolean {
        val job = activeUploads[jobId] ?: return false
        job.status = UploadStatus.CANCELLED
        job.endTime = System.currentTimeMillis()
            return true
    }


    suspend fun pauseUpload(jobId: String): Boolean {
        val job = activeUploads[jobId] ?: return false
        if (job.status == UploadStatus.IN_PROGRESS) {
            job.status = UploadStatus.PAUSED
            return true
        }

        return false
    }


    suspend fun resumeUpload(jobId: String): Boolean {
        val job = activeUploads[jobId] ?: return false
        if (job.status == UploadStatus.PAUSED) {
            job.status = UploadStatus.PENDING
            uploadQueue.send(jobId)
            return true
        }

        return false
    }


    fun getUploadStatus(jobId: String): UploadJob? {
        return activeUploads[jobId]
    }


    fun getActiveUploads(): List<UploadJob> {
        return activeUploads.values.toList()
    }


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


    private fun startUploadProcessor() {
        serviceScope.launch {
            while (this@FileUploadService.isActive.get()) {
                try {
                    val jobId = uploadQueue.receive()
                    if (concurrentUploads.get() >= maxConcurrent) {
                        uploadQueue.send(jobId)
                        delay(QUEUE_RETRY_DELAY_MS)
                        continue
                    }

                    val job = activeUploads[jobId]
                    if (job == null || job.status != UploadStatus.PENDING) {
                        continue
                    }

                    launch {
                        executeUpload(job)
                    }
                } catch (e: Exception) {
                    delay(ERROR_RETRY_DELAY_MS)
                }
            }
        }
    }


    private suspend fun executeUpload(job: UploadJob) {
        concurrentUploads.incrementAndGet()
        try {
            job.status = UploadStatus.IN_PROGRESS
            job.startTime = System.currentTimeMillis()
            val initResponse = initiateUpload(job)
            if (!initResponse) {
                throw Exception("Failed to initiate upload with PC controller")
            }

            uploadFileChunks(job)
            val verifyResponse = verifyUploadCompletion(job)
            if (!verifyResponse) {
                throw Exception("Upload verification failed")
            }

            job.status = UploadStatus.COMPLETED
            job.endTime = System.currentTimeMillis()
            job.bytesUploaded = job.fileSize
        } catch (e: Exception) {
            job.status = UploadStatus.FAILED
            job.endTime = System.currentTimeMillis()
            job.errorMessage = e.message
            job.retryCount++
            if (job.retryCount <= retryLimit) {
                delay(5000L * job.retryCount)
                job.status = UploadStatus.PENDING
                uploadQueue.send(job.jobId)
            }
        } finally {
            concurrentUploads.decrementAndGet()
        }
    }


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

            webSocketClient?.sendMessage(initMessage)
            true
        } catch (e: Exception) {
            false
        }
    }


    private suspend fun uploadFileChunks(job: UploadJob) {
        val file = File(job.filePath)
        FileInputStream(file).use { inputStream ->
            inputStream.skip(job.resumeOffset)
            val buffer = ByteArray(chunkSize)
            var offset = job.resumeOffset
            var chunkIndex = (offset / chunkSize).toInt()
            while (offset < job.fileSize && job.status == UploadStatus.IN_PROGRESS) {
                val bytesToRead = minOf(chunkSize.toLong(), job.fileSize - offset).toInt()
                val bytesRead = inputStream.read(buffer, 0, bytesToRead)
                if (bytesRead <= 0) break
                val chunkData = buffer.copyOf(bytesRead)
                val encodedData =
                    android.util.Base64.encodeToString(chunkData, android.util.Base64.NO_WRAP)
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

                webSocketClient?.sendMessage(chunkMessage)
                    ?: throw Exception("WebSocket client not available")
                offset += bytesRead
                job.bytesUploaded = offset
                chunkIndex++
                delay(10)
            }
        }
    }


    private suspend fun verifyUploadCompletion(job: UploadJob): Boolean {
        return try {
            val verifyMessage =
                JSONObject().apply {
                    put("type", "upload_verify")
                    put("job_id", job.jobId)
                    put("expected_size", job.fileSize)
                    put("expected_checksum", job.checksum)
                }

            webSocketClient?.sendMessage(verifyMessage)
            true
        } catch (e: Exception) {
            false
        }
    }


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
            0L
        } catch (e: Exception) {
            0L
        }
    }


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


    private fun generateJobId(
        sessionId: String,
        deviceId: String,
        fileName: String,
    ): String {
        val timestamp = System.currentTimeMillis()
        val random = Random.nextInt(1000, 9999)
            return "upload_${sessionId}
_${deviceId}
_${timestamp}
_$random"
    }


    fun shutdown() {
        isActive.set(false)
        activeUploads.values.forEach { job ->
            if (job.status == UploadStatus.IN_PROGRESS) {
                job.status = UploadStatus.CANCELLED
            }
        }

        serviceScope.cancel()
    }
}

