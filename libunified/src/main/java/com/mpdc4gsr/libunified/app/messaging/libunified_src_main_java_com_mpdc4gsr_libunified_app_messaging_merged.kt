// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\messaging' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:38


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\messaging\ReliableMessageService.kt =====

package com.mpdc4gsr.libunified.app.messaging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ReliableMessageService(private val context: Context? = null) {
    companion object {
        private const val TAG = "ReliableMessage"
        private const val DEFAULT_TIMEOUT_MS = 10000L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val CLEANUP_INTERVAL_MS = 60000L
        private const val MESSAGE_EXPIRY_MS = 300000L
    }

    private val messageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sequenceNumber = AtomicLong(0)
    private val pendingMessages = ConcurrentHashMap<String, PendingMessage>()
    private val messageHandlers = ConcurrentHashMap<String, MessageHandler>()
    private var cleanupJob: Job? = null

    data class PendingMessage(
        val messageId: String,
        val messageType: String,
        val content: JSONObject,
        val targetHost: String,
        val targetPort: Int,
        val priority: MessagePriority,
        val timeoutMs: Long,
        val maxRetries: Int,
        val sentAt: Long,
        var retryCount: Int = 0,
        var lastRetryAt: Long = 0,
        val callback: MessageCallback?,
    )

    enum class MessagePriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL,
    }

    interface MessageCallback {
        fun onAcknowledged(messageId: String)
        fun onFailed(
            messageId: String,
            error: String,
        )

        fun onRetrying(
            messageId: String,
            attempt: Int,
        )
    }

    interface MessageHandler {
        fun handleMessage(message: JSONObject): JSONObject?
    }

    interface MessageTransport {
        suspend fun sendMessage(
            host: String,
            port: Int,
            message: JSONObject,
        ): Boolean
    }

    private var transport: MessageTransport? = null
    fun setTransport(transport: MessageTransport) {
        this.transport = transport
    }

    fun initialize() {
        cleanupJob =
            messageScope.launch {
                while (isActive) {
                    cleanupExpiredMessages()
                    delay(CLEANUP_INTERVAL_MS)
                }
            }
        Log.i(TAG, "Reliable messaging service initialized")
    }

    suspend fun sendMessage(
        targetHost: String,
        targetPort: Int,
        messageType: String,
        content: JSONObject,
        priority: MessagePriority = MessagePriority.NORMAL,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        maxRetries: Int = MAX_RETRY_ATTEMPTS,
        callback: MessageCallback? = null,
    ): String {
        val messageId = generateMessageId()
        val sequenceNum = sequenceNumber.incrementAndGet()
        val reliableMessage =
            JSONObject().apply {
                put("message_id", messageId)
                put("sequence_number", sequenceNum)
                put("message_type", messageType)
                put("timestamp", System.currentTimeMillis())
                put("priority", priority.name)
                put("requires_ack", true)
                put("sender_id", getSenderId())
                put("content", content)
            }
        val pendingMessage =
            PendingMessage(
                messageId = messageId,
                messageType = messageType,
                content = reliableMessage,
                targetHost = targetHost,
                targetPort = targetPort,
                priority = priority,
                timeoutMs = timeoutMs,
                maxRetries = maxRetries,
                sentAt = System.currentTimeMillis(),
                callback = callback,
            )
        pendingMessages[messageId] = pendingMessage
        messageScope.launch {
            sendWithRetry(pendingMessage)
        }
        Log.d(TAG, "Queued reliable message: $messageType (ID: $messageId)")
        return messageId
    }

    suspend fun sendUnreliableMessage(
        targetHost: String,
        targetPort: Int,
        messageType: String,
        content: JSONObject,
    ): Boolean {
        val messageId = generateMessageId()
        val sequenceNum = sequenceNumber.incrementAndGet()
        val message =
            JSONObject().apply {
                put("message_id", messageId)
                put("sequence_number", sequenceNum)
                put("message_type", messageType)
                put("timestamp", System.currentTimeMillis())
                put("requires_ack", false)
                put("sender_id", getSenderId())
                put("content", content)
            }
        return transport?.sendMessage(targetHost, targetPort, message) ?: false
    }

    suspend fun processIncomingMessage(message: JSONObject): JSONObject? {
        try {
            val messageId = message.optString("message_id")
            val messageType = message.optString("message_type")
            val requiresAck = message.optBoolean("requires_ack", false)
            val senderId = message.optString("sender_id")
            Log.d(TAG, "Processing incoming message: $messageType (ID: $messageId)")
            if (messageType == "ack") {
                val ackForMessageId = message.optString("ack_for_message_id")
                handleAcknowledgment(ackForMessageId)
                return null
            }
            if (messageType == "nack") {
                val nackForMessageId = message.optString("nack_for_message_id")
                val errorReason = message.optString("error_reason", "Unknown error")
                handleNegativeAcknowledgment(nackForMessageId, errorReason)
                return null
            }
            val handler = messageHandlers[messageType]
            val response = handler?.handleMessage(message)
            if (requiresAck && messageId.isNotEmpty()) {
                val ack = createAcknowledgment(messageId, senderId, response != null)
                return ack
            }
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming message", e)
            val messageId = message.optString("message_id")
            val senderId = message.optString("sender_id")
            if (messageId.isNotEmpty()) {
                return createNegativeAcknowledgment(
                    messageId,
                    senderId,
                    e.message ?: "Processing error"
                )
            }
            return null
        }
    }

    fun registerMessageHandler(
        messageType: String,
        handler: MessageHandler,
    ) {
        messageHandlers[messageType] = handler
        Log.d(TAG, "Registered handler for message type: $messageType")
    }

    fun unregisterMessageHandler(messageType: String) {
        messageHandlers.remove(messageType)
        Log.d(TAG, "Unregistered handler for message type: $messageType")
    }

    fun cancelMessage(messageId: String): Boolean {
        val removed = pendingMessages.remove(messageId)
        if (removed != null) {
            Log.d(TAG, "Cancelled message: $messageId")
            return true
        }
        return false
    }

    fun getPendingMessages(): List<PendingMessage> {
        return pendingMessages.values.toList()
    }

    fun getPendingMessageCount(priority: MessagePriority? = null): Int {
        return if (priority == null) {
            pendingMessages.size
        } else {
            pendingMessages.values.count { it.priority == priority }
        }
    }

    private suspend fun sendWithRetry(pendingMessage: PendingMessage) {
        while (pendingMessage.retryCount <= pendingMessage.maxRetries) {
            try {
                val success =
                    transport?.sendMessage(
                        pendingMessage.targetHost,
                        pendingMessage.targetPort,
                        pendingMessage.content,
                    ) ?: false
                if (success) {
                    pendingMessage.lastRetryAt = System.currentTimeMillis()
                    val ackReceived = waitForAcknowledgment(pendingMessage)
                    if (ackReceived) {
                        return
                    }
                }
                pendingMessage.retryCount++
                if (pendingMessage.retryCount <= pendingMessage.maxRetries) {
                    Log.w(
                        TAG,
                        "Retrying message ${pendingMessage.messageId} (attempt ${pendingMessage.retryCount})"
                    )
                    pendingMessage.callback?.onRetrying(
                        pendingMessage.messageId,
                        pendingMessage.retryCount
                    )
                    val delay = RETRY_DELAY_MS * (1 shl (pendingMessage.retryCount - 1))
                    delay(delay)
                } else {
                    Log.e(
                        TAG,
                        "Message ${pendingMessage.messageId} failed after ${pendingMessage.maxRetries} retries"
                    )
                    pendingMessages.remove(pendingMessage.messageId)
                    pendingMessage.callback?.onFailed(
                        pendingMessage.messageId,
                        "Max retries exceeded"
                    )
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message ${pendingMessage.messageId}", e)
                pendingMessage.retryCount++
                if (pendingMessage.retryCount > pendingMessage.maxRetries) {
                    pendingMessages.remove(pendingMessage.messageId)
                    pendingMessage.callback?.onFailed(
                        pendingMessage.messageId,
                        e.message ?: "Send error"
                    )
                    return
                } else {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
    }

    private suspend fun waitForAcknowledgment(pendingMessage: PendingMessage): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < pendingMessage.timeoutMs) {
            if (!pendingMessages.containsKey(pendingMessage.messageId)) {
                return true
            }
            delay(100)
        }
        return false
    }

    private fun handleAcknowledgment(messageId: String) {
        val pendingMessage = pendingMessages.remove(messageId)
        if (pendingMessage != null) {
            Log.d(TAG, "Received ACK for message: $messageId")
            pendingMessage.callback?.onAcknowledged(messageId)
        }
    }

    private fun handleNegativeAcknowledgment(
        messageId: String,
        errorReason: String,
    ) {
        val pendingMessage = pendingMessages.remove(messageId)
        if (pendingMessage != null) {
            Log.w(TAG, "Received NACK for message $messageId: $errorReason")
            pendingMessage.callback?.onFailed(messageId, errorReason)
        }
    }

    private fun createAcknowledgment(
        messageId: String,
        senderId: String,
        success: Boolean,
    ): JSONObject {
        return JSONObject().apply {
            put("message_type", if (success) "ack" else "nack")
            put("ack_for_message_id", messageId)
            put("timestamp", System.currentTimeMillis())
            put("sender_id", getSenderId())
            if (!success) {
                put("error_reason", "Message processing failed")
            }
        }
    }

    private fun createNegativeAcknowledgment(
        messageId: String,
        senderId: String,
        errorReason: String,
    ): JSONObject {
        return JSONObject().apply {
            put("message_type", "nack")
            put("nack_for_message_id", messageId)
            put("error_reason", errorReason)
            put("timestamp", System.currentTimeMillis())
            put("sender_id", getSenderId())
        }
    }

    private fun cleanupExpiredMessages() {
        val currentTime = System.currentTimeMillis()
        val expiredMessages =
            pendingMessages.values.filter {
                currentTime - it.sentAt > MESSAGE_EXPIRY_MS
            }
        expiredMessages.forEach { message ->
            pendingMessages.remove(message.messageId)
            Log.w(TAG, "Expired message: ${message.messageId}")
            message.callback?.onFailed(message.messageId, "Message expired")
        }
        if (expiredMessages.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${expiredMessages.size} expired messages")
        }
    }

    private fun generateMessageId(): String {
        return UUID.randomUUID().toString()
    }

    private fun getSenderId(): String {
        return if (context != null) {
            val androidId =
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID,
                )
            "${android.os.Build.MODEL}-${androidId ?: UUID.randomUUID().toString()}"
        } else {
            "${android.os.Build.MODEL}-${UUID.randomUUID()}"
        }
    }

    fun shutdown() {
        cleanupJob?.cancel()
        messageScope.cancel()
        pendingMessages.clear()
        messageHandlers.clear()
        Log.i(TAG, "Reliable messaging service shutdown")
    }
}