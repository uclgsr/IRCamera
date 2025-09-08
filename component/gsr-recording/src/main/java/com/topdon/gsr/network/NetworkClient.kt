package com.topdon.gsr.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.topdon.gsr.model.SessionInfo
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.*

/**
 * Network client for communicating with PC Controller
 * Implements device pairing, discovery, and remote measurement initiation
 */
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
    private var useTLS = true // Enable TLS by default
    private var clockOffset: Long = 0 // Time synchronization offset in nanoseconds
    private var deviceId: String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )

    private val heartbeatJob = SupervisorJob()
    private val heartbeatScope = CoroutineScope(Dispatchers.IO + heartbeatJob)

    private val messageHandlers = ConcurrentHashMap<String, (JSONObject) -> Unit>()
    private val discoveredControllers = ConcurrentHashMap<String, ControllerInfo>()
    
    // Error recovery integration
    private lateinit var errorRecoveryManager: NetworkErrorRecoveryManager

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
    }

    private var eventListener: NetworkEventListener? = null

    init {
        // Initialize error recovery manager
        errorRecoveryManager = NetworkErrorRecoveryManager(context, this)
        setupErrorRecoveryListener()
    }

    fun setEventListener(listener: NetworkEventListener?) {
        eventListener = listener
    }

    private fun setupErrorRecoveryListener() {
        errorRecoveryManager.setEventListener(object : NetworkErrorRecoveryManager.RecoveryEventListener {
            override fun onRecoveryStarted(reason: String) {
                Log.i(TAG, "Network recovery started: $reason")
            }

            override fun onRecoveryAttempt(attempt: Int, maxAttempts: Int) {
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
        })
    }

    /**
     * Discover PC Controllers on the same network
     */
    suspend fun discoverControllers(): List<ControllerInfo> =
        withContext(Dispatchers.IO) {
            val controllers = mutableListOf<ControllerInfo>()

            try {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcpInfo = wifiManager.dhcpInfo

                if (dhcpInfo.gateway == 0) {
                    Log.w(TAG, "No gateway found, cannot discover controllers")
                    return@withContext controllers
                }

                val gateway = intToIp(dhcpInfo.gateway)
                val subnet = gateway.substring(0, gateway.lastIndexOf('.'))

                Log.i(TAG, "Scanning subnet: $subnet.x for PC Controllers")

                // Parallel scan of subnet
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

                // Wait for all scans to complete
                jobs.awaitAll().filterNotNull().forEach { controllers.add(it) }

                Log.i(TAG, "Discovery complete: found ${controllers.size} controllers")
            } catch (e: Exception) {
                Log.e(TAG, "Error during controller discovery", e)
                eventListener?.onError("discovery", e.message ?: "Unknown error")
            }

            controllers
        }

    /**
     * Connect to a specific PC Controller with TLS encryption
     */
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
                    // Create TLS connection
                    val trustManager = createTrustAllManager()
                    val sslContext = SSLContext.getInstance("TLSv1.2")
                    sslContext.init(null, arrayOf(trustManager), SecureRandom())
                    
                    val sslSocketFactory = sslContext.socketFactory
                    sslSocket = sslSocketFactory.createSocket(ipAddress, port) as SSLSocket
                    sslSocket?.soTimeout = CONNECTION_TIMEOUT.toInt()
                    
                    // Start handshake
                    sslSocket?.startHandshake()
                    
                    outputStream = DataOutputStream(sslSocket?.getOutputStream())
                    inputStream = DataInputStream(sslSocket?.getInputStream())
                } else {
                    // Fallback to regular socket for development
                    socket = Socket()
                    socket?.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT.toInt())
                    socket?.soTimeout = CONNECTION_TIMEOUT.toInt()

                    outputStream = DataOutputStream(socket?.getOutputStream())
                    inputStream = DataInputStream(socket?.getInputStream())
                }

                isConnected = true

                // Perform time synchronization first
                val syncSuccess = performTimeSync()
                if (!syncSuccess) {
                    Log.w(TAG, "Time synchronization failed, but continuing...")
                }

                // Start message listening
                startMessageListener()

                // Send device registration
                val registrationSuccess = registerDevice()

                if (registrationSuccess) {
                    // Start heartbeat
                    startHeartbeat()

                    val controller =
                        discoveredControllers[ipAddress]
                            ?: ControllerInfo(ipAddress, port, "PC Controller", listOf("recording"))
                    
                    // Record successful connection for error recovery
                    errorRecoveryManager.recordSuccessfulConnection(controller)
                    
                    // Enable auto recovery
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

    /**
     * Disconnect from PC Controller
     */
    fun disconnect() {
        isConnected = false
        heartbeatJob.cancel()
        
        // Disable auto recovery when manually disconnecting
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

    /**
     * Send measurement data to PC Controller
     */
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

    /**
     * Report device status to PC Controller
     */
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

                // Wait for ACK
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
                // Handle session stop request
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

    private suspend fun sendMessage(message: JSONObject) =
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
                if (messageLength > 1024 * 1024) { // 1MB limit
                    throw IOException("Message too large: $messageLength bytes")
                }

                val messageData = ByteArray(messageLength)
                input.readFully(messageData)

                sslSocket?.soTimeout = originalTimeout ?: CONNECTION_TIMEOUT.toInt()
                socket?.soTimeout = originalTimeout ?: CONNECTION_TIMEOUT.toInt()

                JSONObject(String(messageData, Charsets.UTF_8))
            } catch (e: SocketTimeoutException) {
                null // Normal timeout, not an error
            } catch (e: Exception) {
                throw e
            }
        }

    /**
     * Perform NTP-like time synchronization with PC Controller
     */
    private suspend fun performTimeSync(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val attempts = 3
                var totalOffset = 0L
                var successfulAttempts = 0

                repeat(attempts) {
                    val t1 = System.nanoTime() // Client timestamp before request
                    
                    val syncRequest = JSONObject().apply {
                        put("message_type", "time_sync_request")
                        put("device_id", deviceId)
                        put("client_timestamp", t1)
                    }
                    
                    sendMessage(syncRequest)
                    
                    val response = receiveMessage(2000)
                    val t4 = System.nanoTime() // Client timestamp after response
                    
                    if (response?.optString("message_type") == "time_sync_response") {
                        val t2 = response.optLong("server_receive_timestamp") // Server timestamp when request received
                        val t3 = response.optLong("server_send_timestamp") // Server timestamp when response sent
                        
                        // Calculate network delay and clock offset
                        val networkDelay = ((t4 - t1) - (t3 - t2)) / 2
                        val offset = ((t2 - t1) + (t3 - t4)) / 2
                        
                        totalOffset += offset
                        successfulAttempts++
                        
                        Log.d(TAG, "Time sync attempt ${it + 1}: offset=${offset}ns, delay=${networkDelay}ns")
                    }
                    
                    delay(100) // Small delay between attempts
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

    /**
     * Get synchronized timestamp using the calculated clock offset
     */
    fun getSynchronizedTimestamp(): Long {
        return System.nanoTime() + clockOffset
    }

    /**
     * Create a trust-all TLS manager for development (should be replaced with proper certificate validation in production)
     */
    private fun createTrustAllManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    /**
     * Start continuous data streaming to PC Controller
     */
    suspend fun startDataStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false

            try {
                val message = JSONObject().apply {
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

    /**
     * Stop continuous data streaming
     */
    suspend fun stopDataStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false

            try {
                val message = JSONObject().apply {
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

                // Send info query
                val query =
                    JSONObject().apply {
                        put("message_type", "info_query")
                        put("device_id", deviceId)
                    }

                val queryData = query.toString().toByteArray(Charsets.UTF_8)
                output.writeInt(queryData.size)
                output.write(queryData)
                output.flush()

                // Read response
                val responseLength = input.readInt()
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
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcpInfo = wifiManager.dhcpInfo
            return intToIp(dhcpInfo.ipAddress)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local IP address", e)
            return "127.0.0.1"
        }
    }

    fun getDiscoveredControllers(): List<ControllerInfo> = discoveredControllers.values.toList()

    /**
     * Enable/disable TLS encryption (for development/testing)
     */
    fun setTLSEnabled(enabled: Boolean) {
        if (isConnected) {
            Log.w(TAG, "Cannot change TLS setting while connected")
            return
        }
        useTLS = enabled
        Log.i(TAG, "TLS encryption ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Get error recovery manager for advanced configuration
     */
    fun getErrorRecoveryManager(): NetworkErrorRecoveryManager = errorRecoveryManager

    /**
     * Clean up all resources
     */
    fun cleanup() {
        disconnect()
        errorRecoveryManager.cleanup()
        discoveredControllers.clear()
        eventListener = null
    }
}
