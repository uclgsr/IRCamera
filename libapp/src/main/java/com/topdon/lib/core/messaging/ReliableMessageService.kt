package com.topdon.lib.core.messaging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Reliable message delivery service with acknowledgments, retry logic, and ordered delivery.
 * Ensures critical messages are delivered even in unreliable network conditions.
 */
class ReliableMessageService(private val context: Context? = null) {
    companion object {
        private const val TAG = "ReliableMessage"
        private const val DEFAULT_TIMEOUT_MS = 10000L // 10 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val CLEANUP_INTERVAL_MS = 60000L // 1 minute
        private const val MESSAGE_EXPIRY_MS = 300000L // 5 minutes
    }

    private val messageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sequenceNumber = AtomicLong(0)
    
    // Pending messages waiting for acknowledgment
    private val pendingMessages = ConcurrentHashMap<String, PendingMessage>()
    
    // Message handlers for different message types
    private val messageHandlers = ConcurrentHashMap<String, MessageHandler>()
    
    // Cleanup job for expired messages
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
        val callback: MessageCallback?
    )

    enum class MessagePriority {
        LOW,      // Non-critical messages (status updates)
        NORMAL,   // Regular messages (data transfer)
        HIGH,     // Important messages (control commands)
        CRITICAL  // Critical messages (emergency stop, sync)
    }

    interface MessageCallback {
        fun onAcknowledged(messageId: String)
        fun onFailed(messageId: String, error: String)
        fun onRetrying(messageId: String, attempt: Int)
    }

    interface MessageHandler {
        fun handleMessage(message: JSONObject): JSONObject? // Return response or null
    }

    interface MessageTransport {
        suspend fun sendMessage(
            host: String, 
            port: Int, 
            message: JSONObject
        ): Boolean
    }

    private var transport: MessageTransport? = null

    fun setTransport(transport: MessageTransport) {
        this.transport = transport
    }

    /**
     * Initialize the reliable messaging service
     */
    fun initialize() {
        // Start cleanup job for expired messages
        cleanupJob = messageScope.launch {
            while (isActive) {
                cleanupExpiredMessages()
                delay(CLEANUP_INTERVAL_MS)
            }
        }
        
        Log.i(TAG, "Reliable messaging service initialized")
    }

    /**
     * Send a reliable message with automatic retry and acknowledgment
     */
    suspend fun sendMessage(
        targetHost: String,
        targetPort: Int,
        messageType: String,
        content: JSONObject,
        priority: MessagePriority = MessagePriority.NORMAL,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        maxRetries: Int = MAX_RETRY_ATTEMPTS,
        callback: MessageCallback? = null
    ): String {
        
        val messageId = generateMessageId()
        val sequenceNum = sequenceNumber.incrementAndGet()
        
        val reliableMessage = JSONObject().apply {
            put("message_id", messageId)
            put("sequence_number", sequenceNum)
            put("message_type", messageType)
            put("timestamp", System.currentTimeMillis())
            put("priority", priority.name)
            put("requires_ack", true)
            put("sender_id", getSenderId())
            put("content", content)
        }

        val pendingMessage = PendingMessage(
            messageId = messageId,
            messageType = messageType,
            content = reliableMessage,
            targetHost = targetHost,
            targetPort = targetPort,
            priority = priority,
            timeoutMs = timeoutMs,
            maxRetries = maxRetries,
            sentAt = System.currentTimeMillis(),
            callback = callback
        )

        pendingMessages[messageId] = pendingMessage

        // Start sending with retry logic
        messageScope.launch {
            sendWithRetry(pendingMessage)
        }

        Log.d(TAG, "Queued reliable message: $messageType (ID: $messageId)")
        return messageId
    }

    /**
     * Send a message without requiring acknowledgment (fire-and-forget)
     */
    suspend fun sendUnreliableMessage(
        targetHost: String,
        targetPort: Int,
        messageType: String,
        content: JSONObject
    ): Boolean {
        
        val messageId = generateMessageId()
        val sequenceNum = sequenceNumber.incrementAndGet()
        
        val message = JSONObject().apply {
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

    /**
     * Process incoming message and generate acknowledgment if needed
     */
    suspend fun processIncomingMessage(message: JSONObject): JSONObject? {
        try {
            val messageId = message.optString("message_id")
            val messageType = message.optString("message_type")
            val requiresAck = message.optBoolean("requires_ack", false)
            val senderId = message.optString("sender_id")

            Log.d(TAG, "Processing incoming message: $messageType (ID: $messageId)")

            // Handle acknowledgments for our sent messages
            if (messageType == "ack") {
                val ackForMessageId = message.optString("ack_for_message_id")
                handleAcknowledgment(ackForMessageId)
                return null
            }

            // Handle negative acknowledgments (message failed)
            if (messageType == "nack") {
                val nackForMessageId = message.optString("nack_for_message_id")
                val errorReason = message.optString("error_reason", "Unknown error")
                handleNegativeAcknowledgment(nackForMessageId, errorReason)
                return null
            }

            // Process regular messages
            val handler = messageHandlers[messageType]
            val response = handler?.handleMessage(message)

            // Send acknowledgment if required
            if (requiresAck && messageId.isNotEmpty()) {
                val ack = createAcknowledgment(messageId, senderId, response != null)
                
                // Extract sender details for response (this would need to be enhanced
                // to track sender information from the connection)
                // For now, we'll return the ACK to be sent by the caller
                return ack
            }

            return response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming message", e)
            
            // Send NACK for the failed message
            val messageId = message.optString("message_id")
            val senderId = message.optString("sender_id")
            
            if (messageId.isNotEmpty()) {
                return createNegativeAcknowledgment(messageId, senderId, e.message ?: "Processing error")
            }
            
            return null
        }
    }

    /**
     * Register a message handler for a specific message type
     */
    fun registerMessageHandler(messageType: String, handler: MessageHandler) {
        messageHandlers[messageType] = handler
        Log.d(TAG, "Registered handler for message type: $messageType")
    }

    /**
     * Unregister a message handler
     */
    fun unregisterMessageHandler(messageType: String) {
        messageHandlers.remove(messageType)
        Log.d(TAG, "Unregistered handler for message type: $messageType")
    }

    /**
     * Cancel a pending message
     */
    fun cancelMessage(messageId: String): Boolean {
        val removed = pendingMessages.remove(messageId)
        if (removed != null) {
            Log.d(TAG, "Cancelled message: $messageId")
            return true
        }
        return false
    }

    /**
     * Get status of all pending messages
     */
    fun getPendingMessages(): List<PendingMessage> {
        return pendingMessages.values.toList()
    }

    /**
     * Get count of pending messages by priority
     */
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
                val success = transport?.sendMessage(
                    pendingMessage.targetHost,
                    pendingMessage.targetPort,
                    pendingMessage.content
                ) ?: false

                if (success) {
                    pendingMessage.lastRetryAt = System.currentTimeMillis()
                    
                    // Wait for acknowledgment with timeout
                    val ackReceived = waitForAcknowledgment(pendingMessage)
                    
                    if (ackReceived) {
                        return // Success!
                    }
                }

                // Failed or no ACK received, retry if possible
                pendingMessage.retryCount++
                
                if (pendingMessage.retryCount <= pendingMessage.maxRetries) {
                    Log.w(TAG, "Retrying message ${pendingMessage.messageId} (attempt ${pendingMessage.retryCount})")
                    pendingMessage.callback?.onRetrying(pendingMessage.messageId, pendingMessage.retryCount)
                    
                    // Exponential backoff delay
                    val delay = RETRY_DELAY_MS * (1 shl (pendingMessage.retryCount - 1))
                    delay(delay)
                } else {
                    // Exhausted all retries
                    Log.e(TAG, "Message ${pendingMessage.messageId} failed after ${pendingMessage.maxRetries} retries")
                    pendingMessages.remove(pendingMessage.messageId)
                    pendingMessage.callback?.onFailed(pendingMessage.messageId, "Max retries exceeded")
                    return
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message ${pendingMessage.messageId}", e)
                pendingMessage.retryCount++
                
                if (pendingMessage.retryCount > pendingMessage.maxRetries) {
                    pendingMessages.remove(pendingMessage.messageId)
                    pendingMessage.callback?.onFailed(pendingMessage.messageId, e.message ?: "Send error")
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
                // Message was acknowledged and removed
                return true
            }
            delay(100) // Check every 100ms
        }
        
        return false // Timeout
    }

    private fun handleAcknowledgment(messageId: String) {
        val pendingMessage = pendingMessages.remove(messageId)
        if (pendingMessage != null) {
            Log.d(TAG, "Received ACK for message: $messageId")
            pendingMessage.callback?.onAcknowledged(messageId)
        }
    }

    private fun handleNegativeAcknowledgment(messageId: String, errorReason: String) {
        val pendingMessage = pendingMessages.remove(messageId)
        if (pendingMessage != null) {
            Log.w(TAG, "Received NACK for message $messageId: $errorReason")
            pendingMessage.callback?.onFailed(messageId, errorReason)
        }
    }

    private fun createAcknowledgment(messageId: String, senderId: String, success: Boolean): JSONObject {
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

    private fun createNegativeAcknowledgment(messageId: String, senderId: String, errorReason: String): JSONObject {
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
        val expiredMessages = pendingMessages.values.filter { 
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
            // Use Settings.Secure.ANDROID_ID as a stable device identifier
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            "${android.os.Build.MODEL}-${androidId ?: UUID.randomUUID().toString()}"
        } else {
            // Fallback for backwards compatibility - use a generated UUID
            "${android.os.Build.MODEL}-${UUID.randomUUID().toString()}"
        }
    }

    /**
     * Shutdown the service and cleanup resources
     */
    fun shutdown() {
        cleanupJob?.cancel()
        messageScope.cancel()
        pendingMessages.clear()
        messageHandlers.clear()
        Log.i(TAG, "Reliable messaging service shutdown")
    }
}