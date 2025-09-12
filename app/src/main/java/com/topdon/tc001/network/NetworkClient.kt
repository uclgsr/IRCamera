package com.topdon.tc001.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.topdon.gsr.model.SessionInfo
import com.topdon.lib.core.discovery.NetworkDiscoveryService
import com.topdon.lib.core.messaging.ReliableMessageService
import com.topdon.lib.core.security.CertificateManager
import com.topdon.lib.core.sync.TimeSyncService
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.*
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.*

/**
 * Enhanced Network client for communicating with PC Controller
 * Implements secure communication, device discovery, time sync, and reliable messaging
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
    private var isSecureConnection = false
    private var useSecureDefault = true // Default to using a secure connection
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

    // Enhanced networking services
    private val certificateManager = CertificateManager(context)
    private val discoveryService = NetworkDiscoveryService(context)
    private val timeSyncService = TimeSyncService()
    private val reliableMessaging = ReliableMessageService(context)

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

    /**
     * Initialize the enhanced network client with security and discovery services
     */
    fun initialize(): Boolean {
        return try {
            // Initialize certificate manager for secure connections
            val certInitialized = certificateManager.initialize()
            if (!certInitialized) {
                Log.w(TAG, "Certificate manager initialization failed, using insecure connections")
            }

            // Initialize discovery service
            discoveryService.setEventListener(object : NetworkDiscoveryService.DiscoveryEventListener {
                override fun onDeviceDiscovered(device: NetworkDiscoveryService.DiscoveredDevice) {
                    if (device.deviceType == NetworkDiscoveryService.DeviceType.PC_CONTROLLER) {
                        val controller = ControllerInfo(
                            ipAddress = device.ipAddress,
                            port = device.port,
                            deviceName = device.serviceName,
                            capabilities = device.attributes.values.toList()
                        )
                        discoveredControllers[device.ipAddress] = controller
                        eventListener?.onControllerDiscovered(controller)
                    }
                }

                override fun onDeviceLost(serviceName: String) {
                    Log.d(TAG, "Device lost: $serviceName")
                }

                override fun onDiscoveryStarted() {
                    Log.d(TAG, "Network discovery started")
                }

                override fun onDiscoveryStopped() {
                    Log.d(TAG, "Network discovery stopped")
                }

                override fun onError(operation: String, error: String) {
                    Log.e(TAG, "Discovery error in $operation: $error")
                    eventListener?.onError("discovery_$operation", error)
                }
            })

            // Initialize time sync service
            timeSyncService.setListener(object : TimeSyncService.TimeSyncListener {
                override fun onSyncCompleted(result: TimeSyncService.SyncResult) {
                    if (result.isSuccess) {
                        // Note: TimeSyncService provides offset in ms, we use ns internally
                        clockOffset = result.clockOffsetMs * 1_000_000
                        Log.i(TAG, "Time sync completed: offset=${result.clockOffsetMs}ms")
                        eventListener?.onTimeSynchronized(clockOffset)
                    }
                }

                override fun onSyncStarted(targetHost: String) {
                    Log.d(TAG, "Time sync started with $targetHost")
                }

                override fun onSyncError(error: String) {
                    Log.e(TAG, "Time sync error: $error")
                    eventListener?.onError("time_sync", error)
                }
            })

            // Initialize reliable messaging
            reliableMessaging.setTransport(object : ReliableMessageService.MessageTransport {
                override suspend fun sendMessage(host: String, port: Int, message: JSONObject): Boolean {
                    return try {
                        sendDirectMessage(message)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send message via transport", e)
                        false
                    }
                }
            })

            reliableMessaging.initialize()

            Log.i(TAG, "Enhanced network client initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize enhanced network client", e)
            false
        }
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
     * Enhanced discovery using both mDNS service discovery and subnet scanning
     */
    suspend fun discoverControllers(): List<ControllerInfo> =
        withContext(Dispatchers.IO) {
            val controllers = mutableListOf<ControllerInfo>()

            try {
                // Start mDNS service discovery
                Log.i(TAG, "Starting enhanced controller discovery")
                discoveryService.startDiscovery()

                // Wait for discovery to find devices
                delay(5000) // Give mDNS time to discover devices

                // Get devices discovered via mDNS
                val discoveredDevices = discoveryService.getDiscoveredDevicesByType(
                    NetworkDiscoveryService.DeviceType.PC_CONTROLLER
                )

                discoveredDevices.forEach { device ->
                    val controller = ControllerInfo(
                        ipAddress = device.ipAddress,
                        port = device.port,
                        deviceName = device.serviceName,
                        capabilities = device.attributes.values.toList()
                    )
                    controllers.add(controller)
                }

                // If mDNS didn't find anything, fall back to subnet scanning
                if (controllers.isEmpty()) {
                    Log.i(TAG, "No controllers found via mDNS, falling back to subnet scan")
                    val scanResults = performSubnetScan()
                    controllers.addAll(scanResults)
                }

                Log.i(TAG, "Enhanced discovery complete: found ${controllers.size} controllers")
            } catch (e: Exception) {
                Log.e(TAG, "Error during enhanced controller discovery", e)
                eventListener?.onError("discovery", e.message ?: "Unknown error")
            }

            controllers
        }

    /**
     * Fallback subnet scanning (original implementation)
     */
    private suspend fun performSubnetScan(): List<ControllerInfo> =
        withContext(Dispatchers.IO) {
            val controllers = mutableListOf<ControllerInfo>()

            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcpInfo = wifiManager.dhcpInfo

                if (dhcpInfo.gateway == 0) {
                    Log.w(TAG, "No gateway found, cannot scan subnet")
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

                Log.i(TAG, "Subnet scan complete: found ${controllers.size} controllers")
            } catch (e: Exception) {
                Log.e(TAG, "Error during subnet scan", e)
            }

            controllers
        }

    /**
     * Enhanced connection with TLS support and time synchronization
     */
    suspend fun connectToController(
        ipAddress: String,
        port: Int = PC_CONTROLLER_PORT,
        useSecure: Boolean = useSecureDefault
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (isConnected) {
                    disconnect()
                }

                Log.i(TAG, "Connecting to PC Controller at $ipAddress:$port (secure: $useSecure)")

                // Try secure connection first
                if (useSecure) {
                    val sslContext = certificateManager.createSSLContext()
                    if (sslContext != null) {
                        val sslSocketFactory = sslContext.socketFactory
                        sslSocket = sslSocketFactory.createSocket(ipAddress, port) as SSLSocket
                        sslSocket?.soTimeout = CONNECTION_TIMEOUT.toInt()
                        
                        // Perform SSL handshake
                        sslSocket?.startHandshake()
                        
                        outputStream = DataOutputStream(sslSocket?.getOutputStream())
                        inputStream = DataInputStream(sslSocket?.getInputStream())
                        isSecureConnection = true
                        
                        Log.i(TAG, "Secure SSL connection established")
                    } else {
                        Log.w(TAG, "SSL context unavailable, falling back to plaintext")
                        return@withContext connectPlaintext(ipAddress, port)
                    }
                } else {
                    return@withContext connectPlaintext(ipAddress, port)
                }

                isConnected = true
                
                // Start message listening
                startMessageListener()

                // Send device registration with authentication
                val registrationSuccess = registerDeviceSecure()

                if (registrationSuccess) {
                    // Perform time synchronization
                    val syncResult = timeSyncService.synchronizeTime(ipAddress, port)
                    if (syncResult.isSuccess) {
                        clockOffset = syncResult.clockOffsetMs * 1_000_000
                        
                        // Start periodic time sync
                        timeSyncService.startPeriodicSync(ipAddress, port)
                    }

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

                    Log.i(TAG, "Successfully connected with enhanced security to PC Controller")
                    true
                } else {
                    disconnect()
                    false
                }
            } catch (e: SSLException) {
                Log.w(TAG, "SSL connection failed, attempting plaintext fallback", e)
                disconnect()
                connectPlaintext(ipAddress, port)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to PC Controller", e)
                errorRecoveryManager.handleNetworkError("connect", e.message ?: "Connection failed")
                eventListener?.onError("connect", e.message ?: "Connection failed")
                disconnect()
                false
            }
        }

    /**
     * Fallback plaintext connection method
     */
    private suspend fun connectPlaintext(ipAddress: String, port: Int): Boolean {
        return try {
            // Create regular socket
            socket = Socket()
            socket?.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT.toInt())
            socket?.soTimeout = CONNECTION_TIMEOUT.toInt()

            outputStream = DataOutputStream(socket?.getOutputStream())
            inputStream = DataInputStream(socket?.getInputStream())
            isSecureConnection = false
            isConnected = true

            // Start message listening
            startMessageListener()

            // Send device registration (original method)
            val registrationSuccess = registerDevice()

            if (registrationSuccess) {
                // Start heartbeat
                startHeartbeat()

                val controller =
                    discoveredControllers[ipAddress]
                        ?: ControllerInfo(ipAddress, port, "PC Controller", listOf("recording"))
                eventListener?.onConnected(controller)

                Log.i(TAG, "Successfully connected with plaintext to PC Controller")
                true
            } else {
                disconnect()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Plaintext connection failed", e)
            eventListener?.onError("connect", e.message ?: "Connection failed")
            disconnect()
            false
        }
    }

    /**
     * Enhanced disconnect with cleanup
     */
    fun disconnect() {
        isConnected = false
        heartbeatJob.cancel()
        
        // Stop services
        timeSyncService.stopPeriodicSync()
        discoveryService.stopDiscovery()

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
            isSecureConnection = false
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
                        put("timestamp", getSynchronizedTimestamp())
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
                        put("timestamp", getCurrentTimestamp()) // Using system time for status reports
                    }

                sendMessage(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report status", e)
                false
            }
        }

    private suspend fun registerDeviceSecure(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val capabilities =
                    listOf(
                        "gsr",
                        "thermal", 
                        "visual",
                        "audio",
                    )

                val authToken = certificateManager.generateAuthToken()

                val registrationMessage =
                    JSONObject().apply {
                        put("message_type", "device_register")
                        put("device_id", deviceId)
                        put("device_type", "android_phone")
                        put("capabilities", org.json.JSONArray(capabilities))
                        put("ip_address", getLocalIpAddress())
                        put("port", PC_CONTROLLER_PORT)
                        put("auth_token", authToken)
                        put("secure_connection", isSecureConnection)
                        put("timestamp", getSynchronizedTimestamp())
                    }

                sendMessage(registrationMessage)

                // Wait for ACK
                val response = receiveMessage(5000)
                response?.optString("message_type") == "ack" &&
                        response.optString("ack_for") == "device_register"
            } catch (e: Exception) {
                Log.e(TAG, "Secure device registration failed", e)
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
                        put("timestamp", getCurrentTimestamp()) // Using system time for non-secure registration
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
                            put("timestamp", getSynchronizedTimestamp())
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
            val startTime = System.currentTimeMillis()
            
            output.writeInt(messageData.size)
            output.write(messageData)
            output.flush()
            
            // Record data transfer for performance tracking
            errorRecoveryManager.recordDataTransfer(messageData.size.toLong() + 4) // +4 for length prefix
            
            // Record latency if this is a ping-like message
            if (message.optString("message_type") == "device_heartbeat") {
                val latency = System.currentTimeMillis() - startTime
                errorRecoveryManager.recordLatency(latency)
            }
        }

    private suspend fun receiveMessage(timeoutMs: Long): JSONObject? =
        withContext(Dispatchers.IO) {
            val input = inputStream ?: return@withContext null

            try {
                val socketToUse = sslSocket ?: socket
                val originalTimeout = socketToUse?.soTimeout
                socketToUse?.soTimeout = timeoutMs.toInt()

                val messageLength = input.readInt()
                if (messageLength > 1024 * 1024) { // 1MB limit
                    throw IOException("Message too large: $messageLength bytes")
                }

                val messageData = ByteArray(messageLength)
                input.readFully(messageData)

                socketToUse?.soTimeout = originalTimeout ?: CONNECTION_TIMEOUT.toInt()

                JSONObject(String(messageData, Charsets.UTF_8))
            } catch (e: SocketTimeoutException) {
                null // Normal timeout, not an error
            } catch (e: Exception) {
                throw e
            }
        }

    /**
     * Get synchronized timestamp using the calculated clock offset
     */
    fun getSynchronizedTimestamp(): Long {
        return System.nanoTime() + clockOffset
    }
    
    /**
     * Send a message to the connected PC Controller
     */
    suspend fun sendMessage(message: JSONObject): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                if (!isConnected) {
                    Log.w(TAG, "Cannot send message - not connected to PC Controller")
                    return@withContext false
                }
                
                sendMessage(message)
                Log.d(TAG, "Message sent successfully: ${message.optString("message_type", "unknown")}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                errorRecoveryManager.handleNetworkError("send_message", e.message ?: "Send failed")
                false
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
                        capabilities = response.optJSONArray("capabilities")?.let { jsonArray ->
                            (0 until jsonArray.length()).map { jsonArray.getString(it) }
                        } ?: emptyList(),
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
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
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

    private fun getCurrentTimestamp(): String {
        return Instant.now().atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcpInfo = wifiManager.dhcpInfo
            return intToIp(dhcpInfo.ipAddress)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local IP address", e)
            return "127.0.0.1"
        }
    }

    fun getDiscoveredControllers(): List<ControllerInfo> = discoveredControllers.values.toList()

    /**
     * Send message directly through current connection
     */
    private suspend fun sendDirectMessage(message: JSONObject) =
        withContext(Dispatchers.IO) {
            val output = outputStream ?: throw IOException("Not connected")

            val messageData = message.toString().toByteArray(Charsets.UTF_8)
            output.writeInt(messageData.size)
            output.write(messageData)
            output.flush()
        }

    /**
     * Get connection security status
     */
    fun isSecureConnection(): Boolean = isSecureConnection
    fun isConnected(): Boolean = isConnected

    /**
     * Enable/disable TLS encryption (for development/testing)
     */
    fun setSecureConnectionDefault(enabled: Boolean) {
        if (isConnected) {
            Log.w(TAG, "Cannot change security setting while connected")
            return
        }
        useSecureDefault = enabled
        Log.i(TAG, "Secure connection default ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Get error recovery manager for advanced configuration
     */
    fun getErrorRecoveryManager(): NetworkErrorRecoveryManager = errorRecoveryManager

    /**
     * Start device discovery with callback-based result handling
     */
    fun startDiscovery(callback: (Boolean) -> Unit) {
        heartbeatScope.launch {
            try {
                discoverControllers()
                callback(true)
            } catch (e: Exception) {
                Log.e(TAG, "Discovery failed", e)
                callback(false)
            }
        }
    }

    /**
     * Connect to controller with callback-based result handling
     */
    fun connectToController(address: String, port: Int, callback: (Boolean) -> Unit) {
        heartbeatScope.launch {
            try {
                val result = connectToController(address, port, useSecureDefault)
                callback(result)
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                callback(false)
            }
        }
    }

    /**
     * Get current network latency in milliseconds
     */
    fun getLatencyMs(): Long {
        return errorRecoveryManager.getAverageLatency()
    }

    /**
     * Get current throughput in KB/s
     */
    fun getThroughputKBps(): Double {
        return errorRecoveryManager.getThroughputKBps()
    }
    
    /**
     * Cleanup all resources
     */
    fun cleanup() {
        disconnect()
        discoveryService.cleanup()
        timeSyncService.cleanup()
        reliableMessaging.shutdown()
        errorRecoveryManager.cleanup()
        discoveredControllers.clear()
        eventListener = null
    }
}