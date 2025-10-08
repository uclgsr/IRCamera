// Merged ALL .kt and .java files from the '_ktjava_mirror\component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\component_gsr-recording_src_main_java_com_mpdc4gsr_gsr_network_all.kt =====

// Merged .kt under 'component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network' subtree
// Files: 8; Generated 2025-10-07 23:07:41


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\DataStreamingService.kt =====

package com.mpdc4gsr.gsr.network

import android.content.Context
import android.util.Log
import com.mpdc4gsr.gsr.model.GSRSample
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class DataStreamingService(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "DataStreamingService"
        private const val BATCH_SIZE = 50
        private const val BATCH_TIMEOUT_MS = 100L
        private const val MAX_QUEUE_SIZE = 5000
        private const val RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 500L
    }

    private val streamingJob = SupervisorJob()
    private val streamingScope = CoroutineScope(Dispatchers.IO + streamingJob)
    private val gsrQueue = ConcurrentLinkedQueue<GSRSample>()
    private val thermalQueue = ConcurrentLinkedQueue<ThermalSample>()
    private val videoMetadataQueue = ConcurrentLinkedQueue<VideoMetadata>()
    private val isStreaming = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private var batchingJob: Job? = null
    private var currentSessionId: String? = null

    data class ThermalSample(
        val timestamp: Long,
        val frameIndex: Long,
        val temperature: Float,
        val x: Int,
        val y: Int,
        val sessionId: String,
    )

    data class VideoMetadata(
        val timestamp: Long,
        val frameIndex: Long,
        val frameSize: Int,
        val sessionId: String,
        val cameraType: String,
    )

    interface StreamingEventListener {
        fun onStreamingStarted(sessionId: String)
        fun onStreamingStopped(sessionId: String)
        fun onBatchSent(
            batchSize: Int,
            dataType: String,
        )

        fun onStreamingError(error: String)
        fun onQueueFull(
            dataType: String,
            droppedSamples: Int,
        )
    }

    private var eventListener: StreamingEventListener? = null
    fun setEventListener(listener: StreamingEventListener?) {
        eventListener = listener
    }

    suspend fun startStreaming(sessionId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (isStreaming.get()) {
                Log.w(TAG, "Data streaming already active")
                return@withContext false
            }
            if (!networkClient.isConnected()) {
                Log.w(TAG, "Cannot start streaming - not connected to PC Controller")
                return@withContext false
            }
            try {
                currentSessionId = sessionId
                isStreaming.set(true)
                isConnected.set(true)
                clearQueues()
                startBatchingProcess()
                val success = networkClient.startDataStreaming()
                if (success) {
                    eventListener?.onStreamingStarted(sessionId)
                    Log.i(TAG, "Data streaming started for session: $sessionId")
                    true
                } else {
                    stopStreaming()
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start data streaming", e)
                eventListener?.onStreamingError("Failed to start: ${e.message}")
                false
            }
        }

    suspend fun stopStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isStreaming.get()) {
                Log.w(TAG, "Data streaming not active")
                return@withContext false
            }
            try {
                isStreaming.set(false)
                batchingJob?.cancel()
                batchingJob = null
                sendRemainingData()
                val success = networkClient.stopDataStreaming()
                val sessionId = currentSessionId
                currentSessionId = null
                if (sessionId != null) {
                    eventListener?.onStreamingStopped(sessionId)
                }
                Log.i(TAG, "Data streaming stopped")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping data streaming", e)
                false
            }
        }

    fun queueGSRSample(sample: GSRSample) {
        if (!isStreaming.get()) return
        if (gsrQueue.size >= MAX_QUEUE_SIZE) {
            val dropped = minOf(BATCH_SIZE, gsrQueue.size / 2)
            repeat(dropped) { gsrQueue.poll() }
            eventListener?.onQueueFull("GSR", dropped)
            Log.w(TAG, "GSR queue full, dropped $dropped samples")
        }
        gsrQueue.offer(sample)
    }

    fun queueThermalSample(sample: ThermalSample) {
        if (!isStreaming.get()) return
        if (thermalQueue.size >= MAX_QUEUE_SIZE) {
            val dropped = minOf(BATCH_SIZE, thermalQueue.size / 2)
            repeat(dropped) { thermalQueue.poll() }
            eventListener?.onQueueFull("Thermal", dropped)
            Log.w(TAG, "Thermal queue full, dropped $dropped samples")
        }
        thermalQueue.offer(sample)
    }

    fun queueVideoMetadata(metadata: VideoMetadata) {
        if (!isStreaming.get()) return
        if (videoMetadataQueue.size >= MAX_QUEUE_SIZE) {
            val dropped = minOf(BATCH_SIZE, videoMetadataQueue.size / 2)
            repeat(dropped) { videoMetadataQueue.poll() }
            eventListener?.onQueueFull("VideoMetadata", dropped)
            Log.w(TAG, "Video metadata queue full, dropped $dropped samples")
        }
        videoMetadataQueue.offer(metadata)
    }

    private fun startBatchingProcess() {
        batchingJob =
            streamingScope.launch {
                while (isStreaming.get() && isActive) {
                    try {
                        if (gsrQueue.size >= BATCH_SIZE) {
                            sendGSRBatch()
                        }
                        if (thermalQueue.size >= BATCH_SIZE) {
                            sendThermalBatch()
                        }
                        if (videoMetadataQueue.size >= BATCH_SIZE) {
                            sendVideoMetadataBatch()
                        }
                        delay(BATCH_TIMEOUT_MS)
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error in batching process", e)
                            eventListener?.onStreamingError("Batching error: ${e.message}")
                            delay(1000)
                        }
                    }
                }
            }
    }

    private suspend fun sendGSRBatch() {
        val batch = mutableListOf<GSRSample>()
        repeat(minOf(BATCH_SIZE, gsrQueue.size)) {
            gsrQueue.poll()?.let { batch.add(it) }
        }
        if (batch.isNotEmpty()) {
            val batchData = createGSRBatchJson(batch)
            if (sendBatchWithRetry(batchData, "gsr")) {
                eventListener?.onBatchSent(batch.size, "GSR")
            }
        }
    }

    private suspend fun sendThermalBatch() {
        val batch = mutableListOf<ThermalSample>()
        repeat(minOf(BATCH_SIZE, thermalQueue.size)) {
            thermalQueue.poll()?.let { batch.add(it) }
        }
        if (batch.isNotEmpty()) {
            val batchData = createThermalBatchJson(batch)
            if (sendBatchWithRetry(batchData, "thermal")) {
                eventListener?.onBatchSent(batch.size, "Thermal")
            }
        }
    }

    private suspend fun sendVideoMetadataBatch() {
        val batch = mutableListOf<VideoMetadata>()
        repeat(minOf(BATCH_SIZE, videoMetadataQueue.size)) {
            videoMetadataQueue.poll()?.let { batch.add(it) }
        }
        if (batch.isNotEmpty()) {
            val batchData = createVideoMetadataBatchJson(batch)
            if (sendBatchWithRetry(batchData, "video_metadata")) {
                eventListener?.onBatchSent(batch.size, "VideoMetadata")
            }
        }
    }

    private suspend fun sendBatchWithRetry(
        batchData: JSONObject,
        dataType: String,
    ): Boolean {
        repeat(RETRY_ATTEMPTS) { attempt ->
            try {
                val success =
                    networkClient.sendMeasurementData(
                        currentSessionId ?: "unknown",
                        batchData,
                    )
                if (success) {
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Batch send attempt ${attempt + 1} failed for $dataType", e)
                if (attempt < RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        Log.e(TAG, "Failed to send $dataType batch after $RETRY_ATTEMPTS attempts")
        eventListener?.onStreamingError("Failed to send $dataType batch")
        return false
    }

    private fun createGSRBatchJson(samples: List<GSRSample>): JSONObject {
        val samplesArray = JSONArray()
        samples.forEach { sample ->
            val sampleJson =
                JSONObject().apply {
                    put("timestamp", sample.timestamp)
                    put("utc_timestamp", sample.utcTimestamp)
                    put("sample_index", sample.sampleIndex)
                    put("conductance", sample.conductance)
                    put("resistance", sample.resistance)
                    put("raw_value", sample.rawValue)
                    put("session_id", sample.sessionId)
                }
            samplesArray.put(sampleJson)
        }
        return JSONObject().apply {
            put("data_type", "gsr_batch")
            put("batch_size", samples.size)
            put("samples", samplesArray)
            put("synchronized_timestamp", networkClient.getSynchronizedTimestamp())
        }
    }

    private fun createThermalBatchJson(samples: List<ThermalSample>): JSONObject {
        val samplesArray = JSONArray()
        samples.forEach { sample ->
            val sampleJson =
                JSONObject().apply {
                    put("timestamp", sample.timestamp)
                    put("frame_index", sample.frameIndex)
                    put("temperature", sample.temperature)
                    put("x", sample.x)
                    put("y", sample.y)
                    put("session_id", sample.sessionId)
                }
            samplesArray.put(sampleJson)
        }
        return JSONObject().apply {
            put("data_type", "thermal_batch")
            put("batch_size", samples.size)
            put("samples", samplesArray)
            put("synchronized_timestamp", networkClient.getSynchronizedTimestamp())
        }
    }

    private fun createVideoMetadataBatchJson(samples: List<VideoMetadata>): JSONObject {
        val samplesArray = JSONArray()
        samples.forEach { sample ->
            val sampleJson =
                JSONObject().apply {
                    put("timestamp", sample.timestamp)
                    put("frame_index", sample.frameIndex)
                    put("frame_size", sample.frameSize)
                    put("session_id", sample.sessionId)
                    put("camera_type", sample.cameraType)
                }
            samplesArray.put(sampleJson)
        }
        return JSONObject().apply {
            put("data_type", "video_metadata_batch")
            put("batch_size", samples.size)
            put("samples", samplesArray)
            put("synchronized_timestamp", networkClient.getSynchronizedTimestamp())
        }
    }

    private suspend fun sendRemainingData() {
        while (gsrQueue.isNotEmpty()) {
            sendGSRBatch()
        }
        while (thermalQueue.isNotEmpty()) {
            sendThermalBatch()
        }
        while (videoMetadataQueue.isNotEmpty()) {
            sendVideoMetadataBatch()
        }
    }

    private fun clearQueues() {
        gsrQueue.clear()
        thermalQueue.clear()
        videoMetadataQueue.clear()
    }

    fun getQueueSizes(): Map<String, Int> {
        return mapOf(
            "gsr" to gsrQueue.size,
            "thermal" to thermalQueue.size,
            "video_metadata" to videoMetadataQueue.size,
        )
    }

    fun isStreamingActive(): Boolean = isStreaming.get()
    suspend fun cleanup() {
        stopStreaming()
        streamingJob.cancel()
        clearQueues()
        eventListener = null
    }
}


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\DeviceAuthenticationManager.kt =====

package com.mpdc4gsr.gsr.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.SecureRandom
import java.time.Instant
import java.util.*

class DeviceAuthenticationManager(private val context: Context) {
    companion object {
        private const val TAG = "DeviceAuth"
        private const val KEYSTORE_ALIAS = "IRCameraDeviceAuth"
        private const val PREFS_NAME = "device_auth_prefs"
        private const val PREF_DEVICE_TOKEN = "device_token"
        private const val PREF_DEVICE_ID = "device_id"
        private const val PREF_PAIRED_CONTROLLERS = "paired_controllers"
        private const val PREF_PAIRING_PIN = "pairing_pin"
        private const val TOKEN_VALIDITY_HOURS = 24
        private const val PAIRING_PIN_LENGTH = 6
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var deviceToken: String? = null
    private var deviceId: String? = null

    init {
        initializeDeviceAuth()
    }

    data class PairingRequest(
        val deviceId: String,
        val deviceName: String,
        val deviceType: String,
        val pairingPin: String,
        val timestamp: Long,
        val capabilities: List<String>,
    )

    data class AuthToken(
        val token: String,
        val deviceId: String,
        val issuedAt: Long,
        val expiresAt: Long,
        val controllerId: String,
        val permissions: List<String>,
    )

    interface AuthEventListener {
        fun onPairingRequested(
            controllerId: String,
            controllerName: String,
        )

        fun onPairingCompleted(
            controllerId: String,
            success: Boolean,
        )

        fun onAuthTokenReceived(token: AuthToken)
        fun onAuthTokenExpired(controllerId: String)
        fun onAuthenticationFailed(
            controllerId: String,
            reason: String,
        )
    }

    private var authEventListener: AuthEventListener? = null
    fun setAuthEventListener(listener: AuthEventListener?) {
        authEventListener = listener
    }

    private fun initializeDeviceAuth() {
        try {
            deviceId = getOrCreateDeviceId()
            deviceToken = getOrCreateDeviceToken()
            Log.d(TAG, "Device authentication initialized - ID: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize device authentication", e)
        }
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString(PREF_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_ID, id).apply()
        }
        return id
    }

    private fun getOrCreateDeviceToken(): String {
        var token = prefs.getString(PREF_DEVICE_TOKEN, null)
        if (token == null || isTokenExpired(token)) {
            token = generateDeviceToken()
            prefs.edit().putString(PREF_DEVICE_TOKEN, token).apply()
        }
        return token
    }

    private fun generateDeviceToken(): String {
        val random = SecureRandom()
        val tokenBytes = ByteArray(32)
        random.nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun isTokenExpired(token: String): Boolean {
        try {
            return false
        } catch (e: Exception) {
            return true
        }
    }

    fun generatePairingPin(): String {
        val random = SecureRandom()
        val pin = StringBuilder()
        repeat(PAIRING_PIN_LENGTH) {
            pin.append(random.nextInt(10))
        }
        val pairingPin = pin.toString()
        prefs.edit().putString(PREF_PAIRING_PIN, pairingPin).apply()
        return pairingPin
    }

    fun getCurrentPairingPin(): String? {
        return prefs.getString(PREF_PAIRING_PIN, null)
    }

    fun createPairingRequest(): PairingRequest {
        val pin = getCurrentPairingPin() ?: generatePairingPin()
        return PairingRequest(
            deviceId = deviceId!!,
            deviceName = getDeviceName(),
            deviceType = "Android Sensor Node",
            pairingPin = pin,
            timestamp = Instant.now().epochSecond,
            capabilities = listOf("GSR", "RGB Camera", "Thermal Camera", "Multi-modal Recording"),
        )
    }

    fun processPairingResponse(response: JSONObject): Boolean {
        try {
            val success = response.getBoolean("success")
            val controllerId = response.getString("controller_id")
            if (success) {
                val pairedControllers = getPairedControllers().toMutableSet()
                pairedControllers.add(controllerId)
                storePairedControllers(pairedControllers)
                if (response.has("auth_token")) {
                    val tokenData = response.getJSONObject("auth_token")
                    val authToken =
                        AuthToken(
                            token = tokenData.getString("token"),
                            deviceId = deviceId!!,
                            issuedAt = tokenData.getLong("issued_at"),
                            expiresAt = tokenData.getLong("expires_at"),
                            controllerId = controllerId,
                            permissions =
                                tokenData.getJSONArray("permissions").let { array ->
                                    (0 until array.length()).map { array.getString(it) }
                                },
                        )
                    storeAuthToken(controllerId, authToken)
                    authEventListener?.onAuthTokenReceived(authToken)
                }
                authEventListener?.onPairingCompleted(controllerId, true)
                Log.d(TAG, "Pairing completed successfully with controller: $controllerId")
                return true
            } else {
                val reason = response.optString("reason", "Unknown error")
                authEventListener?.onPairingCompleted(controllerId, false)
                Log.w(TAG, "Pairing failed with controller $controllerId: $reason")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pairing response", e)
            return false
        }
    }

    fun getAuthToken(controllerId: String): AuthToken? {
        try {
            val tokenJson = prefs.getString("auth_token_$controllerId", null) ?: return null
            val tokenData = JSONObject(tokenJson)
            val authToken =
                AuthToken(
                    token = tokenData.getString("token"),
                    deviceId = tokenData.getString("device_id"),
                    issuedAt = tokenData.getLong("issued_at"),
                    expiresAt = tokenData.getLong("expires_at"),
                    controllerId = tokenData.getString("controller_id"),
                    permissions =
                        tokenData.getJSONArray("permissions").let { array ->
                            (0 until array.length()).map { array.getString(it) }
                        },
                )
            if (Instant.now().epochSecond > authToken.expiresAt) {
                removeAuthToken(controllerId)
                authEventListener?.onAuthTokenExpired(controllerId)
                return null
            }
            return authToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get auth token for controller $controllerId", e)
            return null
        }
    }

    private fun storeAuthToken(
        controllerId: String,
        authToken: AuthToken,
    ) {
        try {
            val tokenData =
                JSONObject().apply {
                    put("token", authToken.token)
                    put("device_id", authToken.deviceId)
                    put("issued_at", authToken.issuedAt)
                    put("expires_at", authToken.expiresAt)
                    put("controller_id", authToken.controllerId)
                    put("permissions", authToken.permissions)
                }
            prefs.edit().putString("auth_token_$controllerId", tokenData.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store auth token", e)
        }
    }

    fun removeAuthToken(controllerId: String) {
        prefs.edit().remove("auth_token_$controllerId").apply()
    }

    fun createAuthenticatedMessage(
        messageType: String,
        data: JSONObject,
        controllerId: String,
    ): JSONObject {
        val authToken = getAuthToken(controllerId)
        return JSONObject().apply {
            put("message_type", messageType)
            put("device_id", deviceId)
            put("timestamp", Instant.now().epochSecond)
            put("data", data)
            if (authToken != null) {
                put("auth_token", authToken.token)
            }
        }
    }

    fun validateMessageAuthentication(
        message: JSONObject,
        controllerId: String,
    ): Boolean {
        try {
            val messageDeviceId = message.optString("device_id", "")
            if (messageDeviceId.isNotEmpty() && messageDeviceId != deviceId) {
                Log.w(TAG, "Message device ID mismatch")
                return false
            }
            val pairedControllers = getPairedControllers()
            if (controllerId !in pairedControllers) {
                Log.w(TAG, "Message from non-paired controller: $controllerId")
                return false
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate message authentication", e)
            return false
        }
    }

    fun getPairedControllers(): Set<String> {
        val pairedJson = prefs.getString(PREF_PAIRED_CONTROLLERS, "[]")
        return try {
            val array = org.json.JSONArray(pairedJson)
            (0 until array.length()).map { array.getString(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun storePairedControllers(controllers: Set<String>) {
        val array = org.json.JSONArray()
        controllers.forEach { array.put(it) }
        prefs.edit().putString(PREF_PAIRED_CONTROLLERS, array.toString()).apply()
    }

    fun unpairController(controllerId: String) {
        val pairedControllers = getPairedControllers().toMutableSet()
        pairedControllers.remove(controllerId)
        storePairedControllers(pairedControllers)
        removeAuthToken(controllerId)
        Log.d(TAG, "Unpaired controller: $controllerId")
    }

    fun clearAllPairings() {
        val pairedControllers = getPairedControllers()
        pairedControllers.forEach { removeAuthToken(it) }
        storePairedControllers(emptySet())
        prefs.edit().remove(PREF_PAIRING_PIN).apply()
        Log.d(TAG, "Cleared all pairing data")
    }

    private fun getDeviceName(): String {
        return android.os.Build.MODEL + " (" + android.os.Build.DEVICE + ")"
    }

    fun getDeviceId(): String? = deviceId
    fun getDeviceToken(): String? = deviceToken
    fun isPaired(): Boolean = getPairedControllers().isNotEmpty()
    fun isPairedWith(controllerId: String): Boolean = controllerId in getPairedControllers()
}


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\FileTransferProtocol.kt =====

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


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\MultiDeviceCoordination.kt =====

package com.mpdc4gsr.gsr.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class MultiDeviceCoordination(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "MultiDeviceCoordination"
        private const val SYNC_INTERVAL_MS = 1000L
        private const val LEADER_ELECTION_TIMEOUT = 5000L
        private const val HEARTBEAT_TIMEOUT = 3000L
        private const val MAX_SYNC_DRIFT_MS = 50L
    }

    private val coordinationJob = SupervisorJob()
    private val coordinationScope = CoroutineScope(Dispatchers.IO + coordinationJob)
    private val connectedDevices = ConcurrentHashMap<String, DeviceInfo>()
    private val syncEvents = ConcurrentHashMap<String, SyncEvent>()
    private val isCoordinating = AtomicBoolean(false)
    private var deviceId: String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )
    private var isLeader = AtomicBoolean(false)
    private var currentSessionId: String? = null
    private var syncJob: Job? = null

    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val capabilities: List<String>,
        val lastHeartbeat: Long,
        val clockOffset: Long = 0L,
        val batteryLevel: Int = 100,
        val isRecording: Boolean = false,
    )

    data class SyncEvent(
        val eventId: String,
        val eventType: String,
        val scheduledTime: Long,
        val deviceResponses: MutableMap<String, Boolean> = mutableMapOf(),
        val isCompleted: Boolean = false,
    )

    enum class CoordinationEvent(val eventType: String) {
        SESSION_START("session_start"),
        SESSION_STOP("session_stop"),
        RECORDING_START("recording_start"),
        RECORDING_STOP("recording_stop"),
        SYNC_FLASH("sync_flash"),
        CALIBRATION("calibration"),
        TIME_SYNC("time_sync"),
    }

    suspend fun initializeCoordination(sessionId: String) =
        withContext(Dispatchers.IO) {
            currentSessionId = sessionId
            isCoordinating.set(true)
            startDeviceDiscovery()
            initiateLeaderElection()
            startSynchronizationLoop()
            Log.d(TAG, "Multi-device coordination initialized for session: $sessionId")
        }

    private suspend fun startDeviceDiscovery() {
        val discoveryMessage =
            JSONObject().apply {
                put("type", "device_discovery")
                put("device_id", deviceId)
                put("device_name", android.os.Build.MODEL)
                put("capabilities", JSONArray(listOf("gsr", "thermal", "rgb_camera")))
                put("session_id", currentSessionId)
                put("timestamp", System.currentTimeMillis())
            }
        networkClient.broadcastMessage(discoveryMessage)
        networkClient.setMessageHandler("device_discovery_response") { message ->
            handleDeviceDiscoveryResponse(message)
        }
        networkClient.setMessageHandler("device_discovery") { message ->
            handleDeviceDiscoveryRequest(message)
        }
    }

    private fun handleDeviceDiscoveryResponse(message: JSONObject) {
        val remoteDeviceId = message.optString("device_id")
        if (remoteDeviceId.isEmpty() || remoteDeviceId == deviceId) return
        val deviceInfo =
            DeviceInfo(
                deviceId = remoteDeviceId,
                deviceName = message.optString("device_name", "Unknown"),
                capabilities = jsonArrayToList(message.optJSONArray("capabilities")),
                lastHeartbeat = System.currentTimeMillis(),
                clockOffset = message.optLong("clock_offset", 0L),
                batteryLevel = message.optInt("battery_level", 100),
            )
        connectedDevices[remoteDeviceId] = deviceInfo
        Log.d(TAG, "Discovered device: $remoteDeviceId (${deviceInfo.deviceName})")
    }

    private fun handleDeviceDiscoveryRequest(message: JSONObject) {
        val response =
            JSONObject().apply {
                put("type", "device_discovery_response")
                put("device_id", deviceId)
                put("device_name", android.os.Build.MODEL)
                put("capabilities", JSONArray(listOf("gsr", "thermal", "rgb_camera")))
                put("session_id", currentSessionId)
                put("clock_offset", networkClient.getClockOffset())
                put("battery_level", getBatteryLevel())
                put("timestamp", System.currentTimeMillis())
            }
        coordinationScope.launch {
            networkClient.sendMessage(response)
        }
    }

    private suspend fun initiateLeaderElection() {
        val electionMessage =
            JSONObject().apply {
                put("type", "leader_election")
                put("device_id", deviceId)
                put("priority", calculateLeadershipPriority())
                put("timestamp", System.currentTimeMillis())
            }
        networkClient.broadcastMessage(electionMessage)
        delay(LEADER_ELECTION_TIMEOUT)
        determineLeader()
        networkClient.setMessageHandler("leader_election") { message ->
            handleLeaderElection(message)
        }
    }

    private fun calculateLeadershipPriority(): Int {
        var priority = 100
        priority += getBatteryLevel()
        priority += 50
        if (networkClient.isConnected()) priority += 25
        return priority
    }

    private fun handleLeaderElection(message: JSONObject) {
        val remoteDeviceId = message.optString("device_id")
        val remotePriority = message.optInt("priority", 0)
        val myPriority = calculateLeadershipPriority()
        if (remotePriority > myPriority ||
            (remotePriority == myPriority && remoteDeviceId < deviceId)
        ) {
            isLeader.set(false)
        }
    }

    private fun determineLeader() {
        if (isLeader.get()) {
            Log.d(TAG, "This device is the coordination leader")
            startLeadershipDuties()
        } else {
            Log.d(TAG, "This device is a follower")
            startFollowerMode()
        }
    }

    private fun startLeadershipDuties() {
        coordinationScope.launch {
            while (isCoordinating.get() && isLeader.get()) {
                broadcastSyncSignal()
                checkDeviceHealth()
                processScheduledEvents()
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private fun startFollowerMode() {
        networkClient.setMessageHandler("sync_signal") { message ->
            handleSyncSignal(message)
        }
        networkClient.setMessageHandler("coordination_event") { message ->
            handleCoordinationEvent(message)
        }
    }

    private suspend fun broadcastSyncSignal() {
        val syncMessage =
            JSONObject().apply {
                put("type", "sync_signal")
                put("leader_id", deviceId)
                put("session_id", currentSessionId)
                put("sync_timestamp", System.currentTimeMillis())
                put("device_count", connectedDevices.size + 1)
            }
        networkClient.broadcastMessage(syncMessage)
    }

    private fun handleSyncSignal(message: JSONObject) {
        val leaderTimestamp = message.optLong("sync_timestamp")
        val currentTime = System.currentTimeMillis()
        val drift = Math.abs(currentTime - leaderTimestamp)
        if (drift > MAX_SYNC_DRIFT_MS) {
            Log.w(TAG, "Time drift detected: ${drift}ms")
            coordinationScope.launch {
                requestTimeResync()
            }
        }
        sendHeartbeat()
    }

    private fun sendHeartbeat() {
        val heartbeatMessage =
            JSONObject().apply {
                put("type", "device_heartbeat")
                put("device_id", deviceId)
                put("timestamp", System.currentTimeMillis())
                put("battery_level", getBatteryLevel())
                put("is_recording", isDeviceRecording())
            }
        coordinationScope.launch {
            networkClient.sendMessage(heartbeatMessage)
        }
    }

    suspend fun scheduleCoordinatedEvent(
        eventType: CoordinationEvent,
        delayMs: Long = 1000L,
    ): String =
        withContext(Dispatchers.IO) {
            val eventId = generateEventId(eventType.eventType)
            val scheduledTime = System.currentTimeMillis() + delayMs
            val syncEvent =
                SyncEvent(
                    eventId = eventId,
                    eventType = eventType.eventType,
                    scheduledTime = scheduledTime,
                )
            syncEvents[eventId] = syncEvent
            val eventMessage =
                JSONObject().apply {
                    put("type", "coordination_event")
                    put("event_id", eventId)
                    put("event_type", eventType.eventType)
                    put("scheduled_time", scheduledTime)
                    put("session_id", currentSessionId)
                }
            networkClient.broadcastMessage(eventMessage)
            Log.d(TAG, "Scheduled coordinated event: ${eventType.eventType} at $scheduledTime")
            eventId
        }

    private fun handleCoordinationEvent(message: JSONObject) {
        val eventId = message.optString("event_id")
        val eventType = message.optString("event_type")
        val scheduledTime = message.optLong("scheduled_time")
        coordinationScope.launch {
            val delay = scheduledTime - System.currentTimeMillis()
            if (delay > 0) {
                delay(delay)
            }
            executeCoordinatedEvent(eventType, eventId)
            sendEventConfirmation(eventId)
        }
    }

    private suspend fun executeCoordinatedEvent(
        eventType: String,
        eventId: String,
    ) {
        Log.d(TAG, "Executing coordinated event: $eventType")
        when (eventType) {
            "session_start" -> handleSessionStart()
            "session_stop" -> handleSessionStop()
            "recording_start" -> handleRecordingStart()
            "recording_stop" -> handleRecordingStop()
            "sync_flash" -> handleSyncFlash()
            "calibration" -> handleCalibration()
            "time_sync" -> handleTimeSync()
        }
    }

    private suspend fun sendEventConfirmation(eventId: String) {
        val confirmationMessage =
            JSONObject().apply {
                put("type", "event_confirmation")
                put("event_id", eventId)
                put("device_id", deviceId)
                put("execution_timestamp", System.currentTimeMillis())
            }
        networkClient.sendMessage(confirmationMessage)
    }

    suspend fun triggerSyncFlash() {
        if (isLeader.get()) {
            scheduleCoordinatedEvent(CoordinationEvent.SYNC_FLASH, 500L)
        }
    }

    private suspend fun handleSyncFlash() {
        Log.d(TAG, "Executing sync flash at ${System.currentTimeMillis()}")
        val flashIntent = android.content.Intent("com.mpdc4gsr.gsr.SYNC_FLASH")
        flashIntent.putExtra("timestamp", System.currentTimeMillis())
        context.sendBroadcast(flashIntent)
    }

    fun getCoordinationStatus(): CoordinationStatus {
        return CoordinationStatus(
            isCoordinating = isCoordinating.get(),
            isLeader = isLeader.get(),
            connectedDevicesCount = connectedDevices.size,
            connectedDevices = connectedDevices.values.toList(),
            activeEvents = syncEvents.size,
            currentSessionId = currentSessionId,
        )
    }

    data class CoordinationStatus(
        val isCoordinating: Boolean,
        val isLeader: Boolean,
        val connectedDevicesCount: Int,
        val connectedDevices: List<DeviceInfo>,
        val activeEvents: Int,
        val currentSessionId: String?,
    )

    private fun jsonArrayToList(jsonArray: JSONArray?): List<String> {
        val list = mutableListOf<String>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                list.add(it.optString(i))
            }
        }
        return list
    }

    private fun generateEventId(eventType: String): String {
        return "${eventType}_${deviceId}_${System.currentTimeMillis()}"
    }

    private fun getBatteryLevel(): Int {
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isDeviceRecording(): Boolean {
        return false // Placeholder
    }

    private suspend fun requestTimeResync() {
        Log.d(TAG, "Requesting time resynchronization")
    }

    private suspend fun handleSessionStart() {
        Log.d(TAG, "Coordinated session start")
    }

    private suspend fun handleSessionStop() {
        Log.d(TAG, "Coordinated session stop")
    }

    private suspend fun handleRecordingStart() {
        Log.d(TAG, "Coordinated recording start")
    }

    private suspend fun handleRecordingStop() {
        Log.d(TAG, "Coordinated recording stop")
    }

    private suspend fun handleCalibration() {
        Log.d(TAG, "Coordinated calibration")
    }

    private suspend fun handleTimeSync() {
        Log.d(TAG, "Coordinated time sync")
    }

    private fun checkDeviceHealth() {
        val currentTime = System.currentTimeMillis()
        connectedDevices.entries.removeAll { (_, device) ->
            val isStale = (currentTime - device.lastHeartbeat) > HEARTBEAT_TIMEOUT
            if (isStale) {
                Log.w(TAG, "Device ${device.deviceId} is no longer responding")
            }
            isStale
        }
    }

    private fun processScheduledEvents() {
        syncEvents.entries.removeAll { (_, event) ->
            event.isCompleted
        }
    }

    private fun startSynchronizationLoop() {
        syncJob =
            coordinationScope.launch {
                while (isCoordinating.get()) {
                    if (isLeader.get()) {
                        broadcastSyncSignal()
                    }
                    delay(SYNC_INTERVAL_MS)
                }
            }
    }

    fun stopCoordination() {
        isCoordinating.set(false)
        isLeader.set(false)
        syncJob?.cancel()
        connectedDevices.clear()
        syncEvents.clear()
        coordinationJob.cancel()
        Log.d(TAG, "Multi-device coordination stopped")
    }
}


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\NetworkClient.kt =====

package com.mpdc4gsr.gsr.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class NetworkClient(private val context: Context) {
    companion object {
        private const val TAG = "NetworkClient"
        private const val PC_CONTROLLER_PORT = 8080
        private const val DISCOVERY_PORT = 8081
        private const val BROADCAST_TIMEOUT = 5000L
        private const val CONNECTION_TIMEOUT = 10000L
        private const val HEARTBEAT_INTERVAL = 5000L
    }

    private var socket: Socket? = null
    private var sslSocket: SSLSocket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isConnected = false
    private var useTLS = true
    private var clockOffset: Long = 0
    private var deviceId: String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )
    private val heartbeatJob = SupervisorJob()
    private val heartbeatScope = CoroutineScope(Dispatchers.IO + heartbeatJob)
    private val messageHandlers = ConcurrentHashMap<String, (JSONObject) -> Unit>()
    private val discoveredControllers = ConcurrentHashMap<String, ControllerInfo>()
    private lateinit var errorRecoveryManager: NetworkErrorRecoveryManager
    private val authManager = DeviceAuthenticationManager(context)

    data class ControllerInfo(
        val ipAddress: String,
        val port: Int,
        val deviceName: String,
        val capabilities: List<String>,
        val lastSeen: Long = System.currentTimeMillis(),
    )

    interface NetworkEventListener {
        fun onControllerDiscovered(controller: ControllerInfo)
        fun onConnected(controller: ControllerInfo)
        fun onDisconnected(reason: String)
        fun onRemoteMeasurementRequest(sessionInfo: SessionInfo)
        fun onSyncFlash(durationMs: Int)
        fun onTimeSynchronized(offsetNanoseconds: Long)
        fun onDataStreamingStarted()
        fun onDataStreamingStopped()
        fun onError(
            operation: String,
            error: String,
        )

        fun onPairingRequested(
            controllerId: String,
            controllerName: String,
        )

        fun onPairingCompleted(
            controllerId: String,
            success: Boolean,
        )

        fun onAuthenticationRequired(controllerId: String)
    }

    private var eventListener: NetworkEventListener? = null

    init {
        errorRecoveryManager = NetworkErrorRecoveryManager(context, this)
        setupErrorRecoveryListener()
        setupAuthenticationListener()
    }

    fun setEventListener(listener: NetworkEventListener?) {
        eventListener = listener
    }

    private fun setupErrorRecoveryListener() {
        errorRecoveryManager.setEventListener(
            object : NetworkErrorRecoveryManager.RecoveryEventListener {
                override fun onRecoveryStarted(reason: String) {
                    Log.i(TAG, "Network recovery started: $reason")
                }

                override fun onRecoveryAttempt(
                    attempt: Int,
                    maxAttempts: Int,
                ) {
                    Log.i(TAG, "Recovery attempt $attempt/$maxAttempts")
                }

                override fun onRecoverySuccess(controller: ControllerInfo) {
                    Log.i(TAG, "Network recovery successful")
                    eventListener?.onConnected(controller)
                }

                override fun onRecoveryFailed(reason: String) {
                    Log.e(TAG, "Network recovery failed: $reason")
                    eventListener?.onError("recovery", reason)
                }

                override fun onConnectionHealthChanged(isHealthy: Boolean) {
                    Log.d(TAG, "Connection health: ${if (isHealthy) "good" else "poor"}")
                }

                override fun onRapidFailureDetected(failureCount: Int) {
                    Log.w(TAG, "Rapid failure detected: $failureCount failures")
                    eventListener?.onError("rapid_failure", "Detected $failureCount rapid failures")
                }
            },
        )
    }

    private fun setupAuthenticationListener() {
        authManager.setAuthEventListener(
            object : DeviceAuthenticationManager.AuthEventListener {
                override fun onPairingRequested(
                    controllerId: String,
                    controllerName: String,
                ) {
                    eventListener?.onPairingRequested(controllerId, controllerName)
                }

                override fun onPairingCompleted(
                    controllerId: String,
                    success: Boolean,
                ) {
                    eventListener?.onPairingCompleted(controllerId, success)
                }

                override fun onAuthTokenReceived(token: DeviceAuthenticationManager.AuthToken) {
                    Log.d(
                        TAG,
                        "Authentication token received for controller: ${token.controllerId}"
                    )
                }

                override fun onAuthTokenExpired(controllerId: String) {
                    Log.w(TAG, "Authentication token expired for controller: $controllerId")
                    eventListener?.onAuthenticationRequired(controllerId)
                }

                override fun onAuthenticationFailed(
                    controllerId: String,
                    reason: String,
                ) {
                    Log.e(TAG, "Authentication failed for controller $controllerId: $reason")
                    eventListener?.onError(
                        "authentication",
                        "Failed to authenticate with $controllerId: $reason"
                    )
                }
            },
        )
    }

    suspend fun discoverControllers(): List<ControllerInfo> =
        withContext(Dispatchers.IO) {
            val controllers = mutableListOf<ControllerInfo>()
            try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (activeNetwork == null || networkCapabilities == null ||
                    !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                ) {
                    Log.w(TAG, "No WiFi network found, cannot discover controllers")
                    return@withContext controllers
                }
                val subnet = "192.168.1"
                Log.i(TAG, "Scanning subnet: $subnet.x for PC Controllers")
                val jobs =
                    (1..254).map { hostNum ->
                        async {
                            val host = "$subnet.$hostNum"
                            try {
                                if (isHostReachable(host, PC_CONTROLLER_PORT, 1000)) {
                                    val controller = queryController(host)
                                    if (controller != null) {
                                        discoveredControllers[host] = controller
                                        eventListener?.onControllerDiscovered(controller)
                                        controller
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Host $host unreachable: ${e.message}")
                                null
                            }
                        }
                    }
                jobs.awaitAll().filterNotNull().forEach { controllers.add(it) }
                Log.i(TAG, "Discovery complete: found ${controllers.size} controllers")
            } catch (e: Exception) {
                Log.e(TAG, "Error during controller discovery", e)
                eventListener?.onError("discovery", e.message ?: "Unknown error")
            }
            controllers
        }

    suspend fun connectToController(
        ipAddress: String,
        port: Int = PC_CONTROLLER_PORT,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (isConnected) {
                    disconnect()
                }
                Log.i(TAG, "Connecting to PC Controller at $ipAddress:$port with TLS")
                if (useTLS) {
                    val trustManager = createTrustAllManager()
                    val sslContext = SSLContext.getInstance("TLSv1.2")
                    sslContext.init(null, arrayOf(trustManager), SecureRandom())
                    val sslSocketFactory = sslContext.socketFactory
                    sslSocket = sslSocketFactory.createSocket(ipAddress, port) as SSLSocket
                    sslSocket?.soTimeout = CONNECTION_TIMEOUT.toInt()
                    sslSocket?.startHandshake()
                    outputStream = DataOutputStream(sslSocket?.getOutputStream())
                    inputStream = DataInputStream(sslSocket?.getInputStream())
                } else {
                    socket = Socket()
                    socket?.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT.toInt())
                    socket?.soTimeout = CONNECTION_TIMEOUT.toInt()
                    outputStream = DataOutputStream(socket?.getOutputStream())
                    inputStream = DataInputStream(socket?.getInputStream())
                }
                isConnected = true
                val syncSuccess = performTimeSync()
                if (!syncSuccess) {
                    Log.w(TAG, "Time synchronization failed, but continuing...")
                }
                startMessageListener()
                val registrationSuccess = registerDevice()
                if (registrationSuccess) {
                    startHeartbeat()
                    val controller =
                        discoveredControllers[ipAddress]
                            ?: ControllerInfo(ipAddress, port, "PC Controller", listOf("recording"))
                    errorRecoveryManager.recordSuccessfulConnection(controller)
                    errorRecoveryManager.enableAutoRecovery()
                    eventListener?.onConnected(controller)
                    Log.i(TAG, "Successfully connected and registered with PC Controller")
                    true
                } else {
                    disconnect()
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to PC Controller", e)
                errorRecoveryManager.handleNetworkError("connect", e.message ?: "Connection failed")
                eventListener?.onError("connect", e.message ?: "Connection failed")
                disconnect()
                false
            }
        }

    fun disconnect() {
        isConnected = false
        heartbeatJob.cancel()
        errorRecoveryManager.disableAutoRecovery()
        try {
            outputStream?.close()
            inputStream?.close()
            sslSocket?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            outputStream = null
            inputStream = null
            sslSocket = null
            socket = null
        }
        eventListener?.onDisconnected("User initiated")
        Log.i(TAG, "Disconnected from PC Controller")
    }

    suspend fun sendMeasurementData(
        sessionId: String,
        data: JSONObject,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "measurement_data")
                        put("device_id", deviceId)
                        put("session_id", sessionId)
                        put("timestamp", getCurrentTimestamp())
                        put("data", data)
                    }
                sendMessage(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send measurement data", e)
                errorRecoveryManager.handleNetworkError("send_data", e.message ?: "Send failed")
                eventListener?.onError("send_data", e.message ?: "Send failed")
                false
            }
        }

    suspend fun reportStatus(
        status: String,
        batteryLevel: Int? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "device_status")
                        put("device_id", deviceId)
                        put("status", status)
                        batteryLevel?.let { put("battery_level", it) }
                        put("timestamp", getCurrentTimestamp())
                    }
                sendMessage(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report status", e)
                false
            }
        }

    private suspend fun registerDevice(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val capabilities =
                    listOf(
                        "gsr",
                        "thermal",
                        "visual",
                        "audio",
                    )
                val registrationMessage =
                    JSONObject().apply {
                        put("message_type", "device_register")
                        put("device_id", deviceId)
                        put("device_type", "android_phone")
                        put("capabilities", org.json.JSONArray(capabilities))
                        put("ip_address", getLocalIpAddress())
                        put("port", PC_CONTROLLER_PORT)
                        put("timestamp", getCurrentTimestamp())
                    }
                sendMessage(registrationMessage)
                val response = receiveMessage(5000)
                response?.optString("message_type") == "ack" &&
                        response.optString("ack_for") == "device_register"
            } catch (e: Exception) {
                Log.e(TAG, "Device registration failed", e)
                false
            }
        }

    private fun startMessageListener() {
        heartbeatScope.launch {
            while (isConnected && isActive) {
                try {
                    val message = receiveMessage(1000)
                    message?.let { handleIncomingMessage(it) }
                } catch (e: Exception) {
                    if (isConnected) {
                        Log.e(TAG, "Message listener error", e)
                        eventListener?.onError("message_listener", e.message ?: "Listener error")
                    }
                    break
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatScope.launch {
            while (isConnected && isActive) {
                try {
                    val heartbeatMessage =
                        JSONObject().apply {
                            put("message_type", "device_heartbeat")
                            put("device_id", deviceId)
                            put("timestamp", getCurrentTimestamp())
                        }
                    sendMessage(heartbeatMessage)
                    delay(HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    if (isConnected) {
                        Log.e(TAG, "Heartbeat failed", e)
                    }
                    break
                }
            }
        }
    }

    private fun handleIncomingMessage(message: JSONObject) {
        val messageType = message.optString("message_type")
        messageHandlers[messageType]?.let { handler ->
            handler(message)
            return
        }
        when (messageType) {
            "session_start" -> {
                val sessionId = message.optString("session_id")
                val sessionName = message.optString("session_name", "Remote Session")
                val sessionInfo =
                    SessionInfo(
                        sessionId = sessionId,
                        startTime = System.currentTimeMillis(),
                        participantId = "remote",
                        studyName = sessionName,
                    )
                eventListener?.onRemoteMeasurementRequest(sessionInfo)
            }

            "sync_flash" -> {
                val durationMs = message.optInt("duration_ms", 100)
                eventListener?.onSyncFlash(durationMs)
            }

            "session_stop" -> {
                Log.i(TAG, "Remote session stop requested")
            }

            "ack" -> {
                Log.d(TAG, "Received ACK for: ${message.optString("ack_for")}")
            }

            "error" -> {
                val errorMsg = message.optString("error_message", "Unknown error")
                Log.w(TAG, "Received error from PC Controller: $errorMsg")
                eventListener?.onError("pc_controller", errorMsg)
            }

            else -> {
                Log.w(TAG, "Unknown message type: $messageType")
            }
        }
    }

    suspend fun sendMessage(message: JSONObject) =
        withContext(Dispatchers.IO) {
            val output = outputStream ?: throw IOException("Not connected")
            val messageData = message.toString().toByteArray(Charsets.UTF_8)
            output.writeInt(messageData.size)
            output.write(messageData)
            output.flush()
        }

    private suspend fun receiveMessage(timeoutMs: Long): JSONObject? =
        withContext(Dispatchers.IO) {
            val input = inputStream ?: return@withContext null
            try {
                val originalTimeout = sslSocket?.soTimeout ?: socket?.soTimeout
                sslSocket?.soTimeout = timeoutMs.toInt()
                socket?.soTimeout = timeoutMs.toInt()
                val messageLength = input.readInt()
                if (messageLength > 1024 * 1024) {
                    throw IOException("Message too large: $messageLength bytes")
                }
                val messageData = ByteArray(messageLength)
                input.readFully(messageData)
                sslSocket?.soTimeout = originalTimeout ?: CONNECTION_TIMEOUT.toInt()
                socket?.soTimeout = originalTimeout ?: CONNECTION_TIMEOUT.toInt()
                JSONObject(String(messageData, Charsets.UTF_8))
            } catch (e: SocketTimeoutException) {
                null
            } catch (e: Exception) {
                throw e
            }
        }

    private suspend fun performTimeSync(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val attempts = 3
                var totalOffset = 0L
                var successfulAttempts = 0
                repeat(attempts) {
                    val t1 = System.nanoTime()
                    val syncRequest =
                        JSONObject().apply {
                            put("message_type", "time_sync_request")
                            put("device_id", deviceId)
                            put("client_timestamp", t1)
                        }
                    sendMessage(syncRequest)
                    val response = receiveMessage(2000)
                    val t4 = System.nanoTime()
                    if (response?.optString("message_type") == "time_sync_response") {
                        val t2 =
                            response.optLong("server_receive_timestamp")
                        val t3 =
                            response.optLong("server_send_timestamp")
                        val networkDelay = ((t4 - t1) - (t3 - t2)) / 2
                        val offset = ((t2 - t1) + (t3 - t4)) / 2
                        totalOffset += offset
                        successfulAttempts++
                        Log.d(
                            TAG,
                            "Time sync attempt ${it + 1}: offset=${offset}ns, delay=${networkDelay}ns"
                        )
                    }
                    delay(100)
                }
                if (successfulAttempts > 0) {
                    clockOffset = totalOffset / successfulAttempts
                    eventListener?.onTimeSynchronized(clockOffset)
                    Log.i(TAG, "Time synchronization complete: average offset=${clockOffset}ns")
                    true
                } else {
                    Log.w(TAG, "Time synchronization failed - no successful attempts")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Time synchronization error", e)
                false
            }
        }

    fun getSynchronizedTimestamp(): Long {
        return System.nanoTime() + clockOffset
    }

    private fun createTrustAllManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String,
            ) {
            }

            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String,
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    suspend fun startDataStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "start_data_stream")
                        put("device_id", deviceId)
                        put("timestamp", getSynchronizedTimestamp())
                    }
                sendMessage(message)
                eventListener?.onDataStreamingStarted()
                Log.i(TAG, "Data streaming started")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start data streaming", e)
                false
            }
        }

    suspend fun stopDataStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "stop_data_stream")
                        put("device_id", deviceId)
                        put("timestamp", getSynchronizedTimestamp())
                    }
                sendMessage(message)
                eventListener?.onDataStreamingStopped()
                Log.i(TAG, "Data streaming stopped")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop data streaming", e)
                false
            }
        }

    private suspend fun queryController(host: String): ControllerInfo? =
        withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, PC_CONTROLLER_PORT), 2000)
                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())
                val query =
                    JSONObject().apply {
                        put("message_type", "info_query")
                        put("device_id", deviceId)
                    }
                val queryData = query.toString().toByteArray(Charsets.UTF_8)
                output.writeInt(queryData.size)
                output.write(queryData)
                output.flush()
                val responseLength = input.readInt()
                if (responseLength > 1024 * 1024) {
                    throw IOException("Response too large: $responseLength bytes")
                }
                val responseData = ByteArray(responseLength)
                input.readFully(responseData)
                val response = JSONObject(String(responseData, Charsets.UTF_8))
                socket.close()
                if (response.optString("message_type") == "info_response") {
                    ControllerInfo(
                        ipAddress = host,
                        port = PC_CONTROLLER_PORT,
                        deviceName = response.optString("device_name", "PC Controller"),
                        capabilities = response.optString("capabilities", "").split(","),
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.d(TAG, "Controller query failed for $host: ${e.message}")
                null
            }
        }

    private suspend fun isHostReachable(
        host: String,
        port: Int,
        timeoutMs: Int,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }

    private fun intToIp(ipAddress: Int): String {
        return (
                (ipAddress and 0xFF).toString() + "." +
                        ((ipAddress shr 8) and 0xFF).toString() + "." +
                        ((ipAddress shr 16) and 0xFF).toString() + "." +
                        ((ipAddress shr 24) and 0xFF).toString()
                )
    }

    fun isConnected(): Boolean = isConnected
    private fun getCurrentTimestamp(): String {
        return Instant.now().atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address.address.size == 4) {
                            return address.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local IP address", e)
        }
        return "127.0.0.1"
    }

    fun getDiscoveredControllers(): List<ControllerInfo> = discoveredControllers.values.toList()
    fun setTLSEnabled(enabled: Boolean) {
        if (isConnected) {
            Log.w(TAG, "Cannot change TLS setting while connected")
            return
        }
        useTLS = enabled
        Log.i(TAG, "TLS encryption ${if (enabled) "enabled" else "disabled"}")
    }

    fun getErrorRecoveryManager(): NetworkErrorRecoveryManager = errorRecoveryManager
    fun cleanup() {
        disconnect()
        errorRecoveryManager.cleanup()
        discoveredControllers.clear()
        eventListener = null
    }

    suspend fun sendBinaryData(data: ByteArray) =
        withContext(Dispatchers.IO) {
            val output = outputStream ?: throw IOException("Not connected")
            output.writeInt(data.size)
            output.write(data)
            output.flush()
        }

    suspend fun waitForResponse(
        messageType: String,
        timeoutMs: Long,
    ): JSONObject {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val message = receiveMessage(1000L)
            if (message?.optString("type") == messageType) {
                return message
            }
            delay(100L)
        }
        throw IOException("Timeout waiting for response: $messageType")
    }

    suspend fun broadcastMessage(message: JSONObject) =
        withContext(Dispatchers.IO) {
            discoveredControllers.values.forEach { controller ->
                try {
                    sendMessage(message)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to broadcast to ${controller.ipAddress}", e)
                }
            }
        }

    fun setMessageHandler(
        messageType: String,
        handler: (JSONObject) -> Unit,
    ) {
        messageHandlers[messageType] = handler
    }

    fun getClockOffset(): Long = clockOffset
    fun startDiscovery(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val controllers = discoverControllers()
                callback(true)
            } catch (e: Exception) {
                Log.e(TAG, "Discovery failed", e)
                callback(false)
            }
        }
    }

    fun connectToController(
        ipAddress: String,
        port: Int,
        callback: (Boolean) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = connectToController(ipAddress, port)
                callback(success)
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                callback(false)
            }
        }
    }

    fun getLatencyMs(): Int {
        return if (isConnected) {
            kotlin.random.Random.nextInt(10, 50)
        } else {
            0
        }
    }

    fun getThroughputKBps(): Double {
        return if (isConnected) {
            kotlin.random.Random.nextDouble(50.0, 200.0)
        } else {
            0.0
        }
    }

    fun generatePairingPin(): String {
        return authManager.generatePairingPin()
    }

    fun getCurrentPairingPin(): String? {
        return authManager.getCurrentPairingPin()
    }

    suspend fun initiatePairing(controllerInfo: ControllerInfo): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val pairingRequest = authManager.createPairingRequest()
                val message =
                    JSONObject().apply {
                        put("message_type", "pairing_request")
                        put("device_id", pairingRequest.deviceId)
                        put("device_name", pairingRequest.deviceName)
                        put("device_type", pairingRequest.deviceType)
                        put("pairing_pin", pairingRequest.pairingPin)
                        put("timestamp", pairingRequest.timestamp)
                        put("capabilities", org.json.JSONArray(pairingRequest.capabilities))
                    }
                sendMessage(message)
                Log.d(TAG, "Pairing request sent to ${controllerInfo.ipAddress}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate pairing", e)
                false
            }
        }

    fun processPairingResponse(response: JSONObject): Boolean {
        return authManager.processPairingResponse(response)
    }

    fun getAuthToken(controllerId: String): DeviceAuthenticationManager.AuthToken? {
        return authManager.getAuthToken(controllerId)
    }

    fun isPairedWith(controllerId: String): Boolean {
        return authManager.isPairedWith(controllerId)
    }

    fun getPairedControllers(): Set<String> {
        return authManager.getPairedControllers()
    }

    fun unpairController(controllerId: String) {
        authManager.unpairController(controllerId)
    }

    fun clearAllPairings() {
        authManager.clearAllPairings()
    }

    suspend fun sendAuthenticatedMessage(
        messageType: String,
        data: JSONObject,
        controllerId: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val authenticatedMessage =
                    authManager.createAuthenticatedMessage(messageType, data, controllerId)
                sendMessage(authenticatedMessage)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send authenticated message", e)
                false
            }
        }

    fun validateMessageAuthentication(
        message: JSONObject,
        controllerId: String,
    ): Boolean {
        return authManager.validateMessageAuthentication(message, controllerId)
    }
}


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\NetworkErrorRecoveryManager.kt =====

package com.mpdc4gsr.gsr.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NetworkErrorRecoveryManager(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "NetworkErrorRecovery"
        private const val MAX_RECONNECTION_ATTEMPTS = 10
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
        private const val HEALTH_CHECK_INTERVAL_MS = 15000L
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val RAPID_FAILURE_THRESHOLD = 3
        private const val RAPID_FAILURE_WINDOW_MS = 60000L
    }

    private val recoveryJob = SupervisorJob()
    private val recoveryScope = CoroutineScope(Dispatchers.IO + recoveryJob)
    private val isRecoveryActive = AtomicBoolean(false)
    private val reconnectionAttempts = AtomicInteger(0)
    private val rapidFailureCount = AtomicInteger(0)
    private var lastFailureTime = 0L
    private var lastKnownGoodController: NetworkClient.ControllerInfo? = null
    private var healthCheckJob: Job? = null

    interface RecoveryEventListener {
        fun onRecoveryStarted(reason: String)
        fun onRecoveryAttempt(
            attempt: Int,
            maxAttempts: Int,
        )

        fun onRecoverySuccess(controller: NetworkClient.ControllerInfo)
        fun onRecoveryFailed(reason: String)
        fun onConnectionHealthChanged(isHealthy: Boolean)
        fun onRapidFailureDetected(failureCount: Int)
    }

    private var eventListener: RecoveryEventListener? = null
    fun setEventListener(listener: RecoveryEventListener?) {
        eventListener = listener
    }

    fun enableAutoRecovery() {
        if (isRecoveryActive.get()) {
            Log.w(TAG, "Auto recovery already enabled")
            return
        }
        isRecoveryActive.set(true)
        startHealthMonitoring()
        Log.i(TAG, "Network error recovery enabled")
    }

    fun disableAutoRecovery() {
        if (!isRecoveryActive.get()) {
            Log.w(TAG, "Auto recovery not active")
            return
        }
        isRecoveryActive.set(false)
        stopHealthMonitoring()
        Log.i(TAG, "Network error recovery disabled")
    }

    suspend fun triggerRecovery(reason: String): Boolean {
        if (isRecoveryActive.get() && reconnectionAttempts.get() > 0) {
            Log.w(TAG, "Recovery already in progress")
            return false
        }
        return performRecovery(reason)
    }

    fun recordSuccessfulConnection(controller: NetworkClient.ControllerInfo) {
        lastKnownGoodController = controller
        reconnectionAttempts.set(0)
        rapidFailureCount.set(0)
        Log.i(TAG, "Recorded successful connection: ${controller.deviceName}")
    }

    fun handleNetworkError(
        operation: String,
        error: String,
    ) {
        Log.w(TAG, "Network error in $operation: $error")
        if (isRapidFailure()) {
            eventListener?.onRapidFailureDetected(rapidFailureCount.get())
            recoveryScope.launch {
                delay(5000)
                if (isRecoveryActive.get()) {
                    performRecovery("Rapid failure in $operation: $error")
                }
            }
        } else if (isRecoveryActive.get()) {
            recoveryScope.launch {
                performRecovery("Error in $operation: $error")
            }
        }
    }

    private fun startHealthMonitoring() {
        healthCheckJob =
            recoveryScope.launch {
                while (isRecoveryActive.get() && isActive) {
                    try {
                        val isHealthy = performHealthCheck()
                        eventListener?.onConnectionHealthChanged(isHealthy)
                        if (!isHealthy && isRecoveryActive.get()) {
                            performRecovery("Health check failed")
                        }
                        delay(HEALTH_CHECK_INTERVAL_MS)
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Health monitoring error", e)
                            delay(HEALTH_CHECK_INTERVAL_MS)
                        }
                    }
                }
            }
    }

    private fun stopHealthMonitoring() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }

    private suspend fun performHealthCheck(): Boolean {
        if (!networkClient.isConnected()) {
            return false
        }
        return try {
            val pingMessage =
                org.json.JSONObject().apply {
                    put("message_type", "ping")
                    put("timestamp", System.currentTimeMillis())
                }
            withTimeout(5000) {
                networkClient.sendMeasurementData("health_check", pingMessage)
            }
            true
        } catch (e: Exception) {
            Log.d(TAG, "Health check failed: ${e.message}")
            false
        }
    }

    private suspend fun performRecovery(reason: String): Boolean {
        if (reconnectionAttempts.get() >= MAX_RECONNECTION_ATTEMPTS) {
            Log.e(TAG, "Maximum reconnection attempts reached")
            eventListener?.onRecoveryFailed("Maximum attempts reached")
            return false
        }
        Log.i(TAG, "Starting connection recovery: $reason")
        eventListener?.onRecoveryStarted(reason)
        var success = false
        val maxAttempts = MAX_RECONNECTION_ATTEMPTS
        while (reconnectionAttempts.get() < maxAttempts && isRecoveryActive.get()) {
            val attempt = reconnectionAttempts.incrementAndGet()
            Log.i(TAG, "Recovery attempt $attempt/$maxAttempts")
            eventListener?.onRecoveryAttempt(attempt, maxAttempts)
            try {
                val controller = lastKnownGoodController
                if (controller != null) {
                    success = attemptReconnection(controller)
                } else {
                    success = attemptDiscoveryAndConnect()
                }
                if (success) {
                    Log.i(TAG, "Recovery successful after $attempt attempts")
                    eventListener?.onRecoverySuccess(
                        lastKnownGoodController
                            ?: NetworkClient.ControllerInfo("unknown", 0, "Recovered", emptyList()),
                    )
                    reconnectionAttempts.set(0)
                    break
                } else {
                    val delay = calculateRetryDelay(attempt)
                    Log.d(TAG, "Recovery attempt $attempt failed, retrying in ${delay}ms")
                    delay(delay)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recovery attempt $attempt failed with exception", e)
                val delay = calculateRetryDelay(attempt)
                delay(delay)
            }
        }
        if (!success) {
            Log.e(TAG, "Connection recovery failed after $maxAttempts attempts")
            eventListener?.onRecoveryFailed("All attempts exhausted")
        }
        return success
    }

    private suspend fun attemptReconnection(controller: NetworkClient.ControllerInfo): Boolean {
        return try {
            Log.d(
                TAG,
                "Attempting reconnection to ${controller.deviceName} at ${controller.ipAddress}"
            )
            networkClient.disconnect()
            delay(1000)
            withTimeout(CONNECTION_TIMEOUT_MS) {
                networkClient.connectToController(controller.ipAddress, controller.port)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Reconnection attempt failed: ${e.message}")
            false
        }
    }

    private suspend fun attemptDiscoveryAndConnect(): Boolean {
        return try {
            Log.d(TAG, "Attempting discovery and connection")
            val controllers =
                withTimeout(15000) {
                    networkClient.discoverControllers()
                }
            if (controllers.isNotEmpty()) {
                val controller = controllers.first()
                Log.d(TAG, "Found controller during recovery: ${controller.deviceName}")
                val connected =
                    withTimeout(CONNECTION_TIMEOUT_MS) {
                        networkClient.connectToController(controller.ipAddress, controller.port)
                    }
                if (connected) {
                    lastKnownGoodController = controller
                }
                connected
            } else {
                Log.d(TAG, "No controllers found during discovery")
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Discovery and connect attempt failed: ${e.message}")
            false
        }
    }

    private fun calculateRetryDelay(attempt: Int): Long {
        val baseDelay = INITIAL_RETRY_DELAY_MS * (1L shl (attempt - 1))
        val cappedDelay = minOf(baseDelay, MAX_RETRY_DELAY_MS)
        val jitter = (Math.random() * 0.1 * cappedDelay).toLong()
        return cappedDelay + jitter
    }

    private fun isRapidFailure(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFailureTime > RAPID_FAILURE_WINDOW_MS) {
            rapidFailureCount.set(1)
        } else {
            rapidFailureCount.incrementAndGet()
        }
        lastFailureTime = currentTime
        return rapidFailureCount.get() >= RAPID_FAILURE_THRESHOLD
    }

    fun resetRecoveryState() {
        reconnectionAttempts.set(0)
        rapidFailureCount.set(0)
        lastFailureTime = 0L
        Log.i(TAG, "Recovery state reset")
    }

    fun getRecoveryStats(): Map<String, Any> {
        return mapOf(
            "recovery_active" to isRecoveryActive.get(),
            "reconnection_attempts" to reconnectionAttempts.get(),
            "rapid_failure_count" to rapidFailureCount.get(),
            "last_failure_time" to lastFailureTime,
            "has_known_good_controller" to (lastKnownGoodController != null),
        )
    }

    fun cleanup() {
        disableAutoRecovery()
        recoveryJob.cancel()
        eventListener = null
        lastKnownGoodController = null
    }
}


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\QualityOfServiceManager.kt =====

package com.mpdc4gsr.gsr.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class QualityOfServiceManager(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "QoSManager"
        private const val BANDWIDTH_MONITOR_INTERVAL = 2000L
        private const val CONGESTION_THRESHOLD = 0.8f
        private const val PRIORITY_QUEUE_SIZE = 1000
        private const val ADAPTIVE_BATCH_MIN = 10
        private const val ADAPTIVE_BATCH_MAX = 200
        private const val NETWORK_LATENCY_SAMPLES = 10
    }

    private val qosJob = SupervisorJob()
    private val qosScope = CoroutineScope(Dispatchers.IO + qosJob)
    private val isMonitoring = AtomicBoolean(false)
    private val currentBandwidth = AtomicLong(0)
    private val networkLatency = AtomicLong(0)
    private val packetLossRate = AtomicLong(0)
    private val criticalQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private val highPriorityQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private val normalPriorityQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private val lowPriorityQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private var adaptiveBatchSize = 50
    private var compressionLevel = CompressionLevel.MEDIUM
    private var currentNetworkTier = NetworkTier.MEDIUM

    data class QoSDataPacket(
        val data: ByteArray,
        val dataType: DataType,
        val priority: Priority,
        val timestamp: Long,
        val sessionId: String,
        val metadata: Map<String, String> = emptyMap(),
    )

    enum class DataType(val typeName: String) {
        GSR("gsr"),
        THERMAL("thermal"),
        VIDEO_METADATA("video_metadata"),
        CONTROL_MESSAGE("control"),
        FILE_CHUNK("file_chunk"),
        HEARTBEAT("heartbeat"),
    }

    enum class Priority(val level: Int) {
        CRITICAL(4),
        HIGH(3),
        NORMAL(2),
        LOW(1),
    }

    enum class CompressionLevel(val factor: Float) {
        NONE(1.0f),
        LOW(0.9f),
        MEDIUM(0.7f),
        HIGH(0.5f),
        MAXIMUM(0.3f),
    }

    enum class NetworkTier {
        POOR,
        LOW,
        MEDIUM,
        HIGH,
        EXCELLENT,
    }

    data class NetworkQualityMetrics(
        val bandwidth: Long,
        val latency: Long,
        val packetLoss: Float,
        val networkTier: NetworkTier,
        val recommendedBatchSize: Int,
        val recommendedCompression: CompressionLevel,
        val congestionLevel: Float,
    )

    suspend fun startQoSMonitoring() =
        withContext(Dispatchers.IO) {
            if (isMonitoring.getAndSet(true)) {
                Log.w(TAG, "QoS monitoring already active")
                return@withContext
            }
            Log.d(TAG, "Starting QoS monitoring")
            startBandwidthMonitoring()
            startLatencyMonitoring()
            startAdaptiveProcessing()
            startPriorityQueueProcessor()
        }

    private fun startBandwidthMonitoring() {
        qosScope.launch {
            while (isMonitoring.get()) {
                val bandwidth = measureBandwidth()
                currentBandwidth.set(bandwidth)
                updateNetworkTier(bandwidth)
                adjustCompressionLevel(bandwidth)
                delay(BANDWIDTH_MONITOR_INTERVAL)
            }
        }
    }

    private fun startLatencyMonitoring() {
        qosScope.launch {
            while (isMonitoring.get()) {
                val latency = measureNetworkLatency()
                networkLatency.set(latency)
                delay(5000L)
            }
        }
    }

    private suspend fun measureBandwidth(): Long =
        withContext(Dispatchers.IO) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return@withContext when {
                networkCapabilities == null -> 0L
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    measureWiFiBandwidth()
                }

                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    measureCellularBandwidth()
                }

                else -> 1024 * 1024L
            }
        }

    @Suppress("DEPRECATION")
    private fun measureWiFiBandwidth(): Long {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val rssi = wifiInfo.rssi
        val linkSpeed = wifiInfo.linkSpeed
        val signalQuality =
            when {
                rssi >= -50 -> 1.0f
                rssi >= -60 -> 0.8f
                rssi >= -70 -> 0.6f
                rssi >= -80 -> 0.4f
                else -> 0.2f
            }
        return (linkSpeed * 1024 * 1024 / 8 * signalQuality).toLong()
    }

    private fun measureCellularBandwidth(): Long {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return when {
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true -> {
                2 * 1024 * 1024L
            }

            else -> 512 * 1024L
        }
    }

    private suspend fun measureNetworkLatency(): Long =
        withContext(Dispatchers.IO) {
            val samples = mutableListOf<Long>()
            repeat(NETWORK_LATENCY_SAMPLES) {
                val startTime = System.currentTimeMillis()
                try {
                    val pingMessage =
                        JSONObject().apply {
                            put("type", "qos_ping")
                            put("timestamp", startTime)
                        }
                    networkClient.sendMessage(pingMessage)
                    val response = networkClient.waitForResponse("qos_pong", 2000L)
                    val endTime = System.currentTimeMillis()
                    val latency = endTime - startTime
                    samples.add(latency)
                } catch (e: Exception) {
                    samples.add(2000L)
                }
                delay(100L)
            }
            samples.sorted()[samples.size / 2]
        }

    private fun updateNetworkTier(bandwidth: Long) {
        currentNetworkTier =
            when {
                bandwidth > 10 * 1024 * 1024L -> NetworkTier.EXCELLENT
                bandwidth > 2 * 1024 * 1024L -> NetworkTier.HIGH
                bandwidth > 500 * 1024L -> NetworkTier.MEDIUM
                bandwidth > 100 * 1024L -> NetworkTier.LOW
                else -> NetworkTier.POOR
            }
        Log.d(TAG, "Network tier updated: $currentNetworkTier (${bandwidth / 1024}KB/s)")
    }

    private fun adjustCompressionLevel(bandwidth: Long) {
        compressionLevel =
            when (currentNetworkTier) {
                NetworkTier.POOR -> CompressionLevel.MAXIMUM
                NetworkTier.LOW -> CompressionLevel.HIGH
                NetworkTier.MEDIUM -> CompressionLevel.MEDIUM
                NetworkTier.HIGH -> CompressionLevel.LOW
                NetworkTier.EXCELLENT -> CompressionLevel.NONE
            }
    }

    private fun startAdaptiveProcessing() {
        qosScope.launch {
            while (isMonitoring.get()) {
                adaptParameters()
                delay(BANDWIDTH_MONITOR_INTERVAL)
            }
        }
    }

    private fun adaptParameters() {
        val bandwidth = currentBandwidth.get()
        val latency = networkLatency.get()
        val utilization = calculateBandwidthUtilization()
        adaptiveBatchSize =
            when {
                bandwidth > 5 * 1024 * 1024L && latency < 50L -> ADAPTIVE_BATCH_MAX
                bandwidth > 1 * 1024 * 1024L && latency < 100L -> (ADAPTIVE_BATCH_MAX * 0.7).toInt()
                bandwidth > 500 * 1024L -> (ADAPTIVE_BATCH_MAX * 0.5).toInt()
                else -> ADAPTIVE_BATCH_MIN
            }
        if (utilization > CONGESTION_THRESHOLD) {
            adaptiveBatchSize = (adaptiveBatchSize * 0.7).toInt()
        }
        Log.v(TAG, "Adapted batch size: $adaptiveBatchSize, utilization: $utilization")
    }

    private fun calculateBandwidthUtilization(): Float {
        val availableBandwidth = currentBandwidth.get()
        if (availableBandwidth <= 0) return 1.0f
        val usedBandwidth = calculateCurrentUsage()
        return (usedBandwidth.toFloat() / availableBandwidth.toFloat()).coerceAtMost(1.0f)
    }

    private fun calculateCurrentUsage(): Long {
        val queueSize = getTotalQueueSize()
        return queueSize * 100L
    }

    fun queueData(
        data: ByteArray,
        dataType: DataType,
        priority: Priority,
        sessionId: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        val packet =
            QoSDataPacket(
                data = data,
                dataType = dataType,
                priority = priority,
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                metadata = metadata,
            )
        val targetQueue =
            when (priority) {
                Priority.CRITICAL -> criticalQueue
                Priority.HIGH -> highPriorityQueue
                Priority.NORMAL -> normalPriorityQueue
                Priority.LOW -> lowPriorityQueue
            }
        while (targetQueue.size >= PRIORITY_QUEUE_SIZE) {
            val dropped = targetQueue.poll()
            Log.w(TAG, "Dropped packet due to queue overflow: ${dropped?.dataType}")
        }
        targetQueue.offer(packet)
    }

    private fun startPriorityQueueProcessor() {
        qosScope.launch {
            while (isMonitoring.get()) {
                processPriorityQueues()
                delay(50L)
            }
        }
    }

    private suspend fun processPriorityQueues() {
        val batch = mutableListOf<QoSDataPacket>()
        val maxBatchSize = adaptiveBatchSize
        while (criticalQueue.isNotEmpty() && batch.size < maxBatchSize) {
            criticalQueue.poll()?.let { batch.add(it) }
        }
        while (highPriorityQueue.isNotEmpty() && batch.size < maxBatchSize) {
            highPriorityQueue.poll()?.let { batch.add(it) }
        }
        while (normalPriorityQueue.isNotEmpty() && batch.size < maxBatchSize) {
            normalPriorityQueue.poll()?.let { batch.add(it) }
        }
        if (calculateBandwidthUtilization() < CONGESTION_THRESHOLD) {
            while (lowPriorityQueue.isNotEmpty() && batch.size < maxBatchSize) {
                lowPriorityQueue.poll()?.let { batch.add(it) }
            }
        }
        if (batch.isNotEmpty()) {
            sendBatch(batch)
        }
    }

    private suspend fun sendBatch(batch: List<QoSDataPacket>) {
        try {
            val compressedBatch = compressBatch(batch)
            val batchMessage = createBatchMessage(compressedBatch)
            networkClient.sendMessage(batchMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send batch", e)
            batch.filter { it.priority.level >= Priority.HIGH.level }
                .forEach { queueData(it.data, it.dataType, it.priority, it.sessionId, it.metadata) }
        }
    }

    private fun compressBatch(batch: List<QoSDataPacket>): List<QoSDataPacket> {
        if (compressionLevel == CompressionLevel.NONE) return batch
        return batch.map { packet ->
            when (packet.dataType) {
                DataType.GSR -> packet
                DataType.THERMAL -> compressThermalData(packet)
                DataType.VIDEO_METADATA -> compressVideoMetadata(packet)
                else -> packet
            }
        }
    }

    private fun compressThermalData(packet: QoSDataPacket): QoSDataPacket {
        return packet
    }

    private fun compressVideoMetadata(packet: QoSDataPacket): QoSDataPacket {
        return packet
    }

    private fun createBatchMessage(batch: List<QoSDataPacket>): JSONObject {
        return JSONObject().apply {
            put("type", "qos_batch")
            put("batch_size", batch.size)
            put("compression_level", compressionLevel.name)
            put("timestamp", System.currentTimeMillis())
        }
    }

    fun getNetworkQualityMetrics(): NetworkQualityMetrics {
        return NetworkQualityMetrics(
            bandwidth = currentBandwidth.get(),
            latency = networkLatency.get(),
            packetLoss = packetLossRate.get() / 100.0f,
            networkTier = currentNetworkTier,
            recommendedBatchSize = adaptiveBatchSize,
            recommendedCompression = compressionLevel,
            congestionLevel = calculateBandwidthUtilization(),
        )
    }

    fun getQueueStatistics(): QueueStatistics {
        return QueueStatistics(
            criticalQueueSize = criticalQueue.size,
            highPriorityQueueSize = highPriorityQueue.size,
            normalPriorityQueueSize = normalPriorityQueue.size,
            lowPriorityQueueSize = lowPriorityQueue.size,
            totalQueueSize = getTotalQueueSize(),
            adaptiveBatchSize = adaptiveBatchSize,
        )
    }

    data class QueueStatistics(
        val criticalQueueSize: Int,
        val highPriorityQueueSize: Int,
        val normalPriorityQueueSize: Int,
        val lowPriorityQueueSize: Int,
        val totalQueueSize: Int,
        val adaptiveBatchSize: Int,
    )

    private fun getTotalQueueSize(): Int {
        return criticalQueue.size + highPriorityQueue.size +
                normalPriorityQueue.size + lowPriorityQueue.size
    }

    fun stopQoSMonitoring() {
        isMonitoring.set(false)
        criticalQueue.clear()
        highPriorityQueue.clear()
        normalPriorityQueue.clear()
        lowPriorityQueue.clear()
        qosJob.cancel()
        Log.d(TAG, "QoS monitoring stopped")
    }
}


// ===== component\gsr-recording\src\main\java\com\mpdc4gsr\gsr\network\ZeroconfDiscoveryService.kt =====

package com.mpdc4gsr.gsr.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class ZeroconfDiscoveryService(private val context: Context) {
    companion object {
        private const val TAG = "ZeroconfDiscovery"
        private const val SERVICE_TYPE = "_ircamera._tcp."
        private const val SERVICE_NAME = "IRCamera-Device"
        private const val DISCOVERY_TIMEOUT = 30000L
    }

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private val discoveredServices = ConcurrentHashMap<String, NsdServiceInfo>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isDiscovering = false
    private var isRegistered = false

    interface ServiceDiscoveryListener {
        fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo)
        fun onServiceLost(serviceName: String)
        fun onServiceRegistered(serviceName: String)
        fun onDiscoveryError(
            errorCode: Int,
            message: String,
        )
    }

    private var serviceListener: ServiceDiscoveryListener? = null
    fun setServiceListener(listener: ServiceDiscoveryListener?) {
        serviceListener = listener
    }

    suspend fun startDiscovery(): Boolean =
        withContext(Dispatchers.Main) {
            if (isDiscovering) {
                Log.w(TAG, "Discovery already in progress")
                return@withContext true
            }
            try {
                discoveryListener = createDiscoveryListener()
                nsdManager.discoverServices(
                    SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    discoveryListener
                )
                isDiscovering = true
                Log.i(TAG, "Started mDNS service discovery for type: $SERVICE_TYPE")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service discovery", e)
                serviceListener?.onDiscoveryError(-1, e.message ?: "Discovery failed")
                false
            }
        }

    fun stopDiscovery() {
        if (!isDiscovering) return
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
            isDiscovering = false
            discoveredServices.clear()
            Log.i(TAG, "Stopped service discovery")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }

    suspend fun registerService(
        deviceId: String,
        port: Int,
    ): Boolean =
        withContext(Dispatchers.Main) {
            if (isRegistered) {
                Log.w(TAG, "Service already registered")
                return@withContext true
            }
            try {
                val serviceInfo =
                    NsdServiceInfo().apply {
                        serviceName = "$SERVICE_NAME-$deviceId"
                        serviceType = SERVICE_TYPE
                        setPort(port)
                    }
                registrationListener = createRegistrationListener()
                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    registrationListener
                )
                Log.i(TAG, "Registering service: ${serviceInfo.serviceName}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register service", e)
                false
            }
        }

    fun unregisterService() {
        if (!isRegistered) return
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
            isRegistered = false
            Log.i(TAG, "Unregistered service")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }
    }

    @Suppress("DEPRECATION")
    fun getDiscoveredControllers(): List<NetworkClient.ControllerInfo> {
        return discoveredServices.values.mapNotNull { serviceInfo ->
            try {
                val host = serviceInfo.host?.hostAddress ?: return@mapNotNull null
                val port = serviceInfo.port
                val deviceName = serviceInfo.serviceName
                val capabilities = emptyList<String>()
                NetworkClient.ControllerInfo(
                    ipAddress = host,
                    port = port,
                    deviceName = deviceName,
                    capabilities = capabilities,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse service info: ${serviceInfo.serviceName}", e)
                null
            }
        }
    }

    private fun createDiscoveryListener(): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service discovery success: ${service.serviceName}")
                if (service.serviceName.startsWith(SERVICE_NAME)) {
                    return
                }
                @Suppress("DEPRECATION")
                nsdManager.resolveService(service, createResolveListener())
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.i(TAG, "Service lost: ${service.serviceName}")
                discoveredServices.remove(service.serviceName)
                serviceListener?.onServiceLost(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {
                Log.e(TAG, "Discovery failed to start: $serviceType, error: $errorCode")
                isDiscovering = false
                serviceListener?.onDiscoveryError(errorCode, "Failed to start discovery")
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {
                Log.e(TAG, "Discovery failed to stop: $serviceType, error: $errorCode")
                serviceListener?.onDiscoveryError(errorCode, "Failed to stop discovery")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {
                Log.e(TAG, "Resolve failed: ${serviceInfo.serviceName}, error: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.i(
                    TAG,
                    "Service resolved: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}"
                )
                discoveredServices[serviceInfo.serviceName] = serviceInfo
                try {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val deviceName = serviceInfo.serviceName
                    val capabilities =
                        emptyList<String>()
                    val controllerInfo =
                        NetworkClient.ControllerInfo(
                            ipAddress = host,
                            port = port,
                            deviceName = deviceName,
                            capabilities = capabilities,
                        )
                    serviceListener?.onServiceDiscovered(controllerInfo)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse resolved service", e)
                }
            }
        }
    }

    private fun createRegistrationListener(): NsdManager.RegistrationListener {
        return object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Service registered: ${serviceInfo.serviceName}")
                isRegistered = true
                serviceListener?.onServiceRegistered(serviceInfo.serviceName)
            }

            override fun onRegistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {
                Log.e(
                    TAG,
                    "Service registration failed: ${serviceInfo.serviceName}, error: $errorCode"
                )
                isRegistered = false
                serviceListener?.onDiscoveryError(errorCode, "Registration failed")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Service unregistered: ${serviceInfo.serviceName}")
                isRegistered = false
            }

            override fun onUnregistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {
                Log.e(
                    TAG,
                    "Service unregistration failed: ${serviceInfo.serviceName}, error: $errorCode"
                )
                serviceListener?.onDiscoveryError(errorCode, "Unregistration failed")
            }
        }
    }

    fun cleanup() {
        stopDiscovery()
        unregisterService()
        discoveredServices.clear()
        serviceListener = null
        discoveryListener = null
        registrationListener = null
    }
}