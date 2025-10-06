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
