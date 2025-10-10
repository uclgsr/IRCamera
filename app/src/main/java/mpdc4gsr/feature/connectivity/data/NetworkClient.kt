package mpdc4gsr.feature.connectivity.data

import android.content.Context
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Process
import com.mpdc4gsr.libunified.app.discovery.NetworkDiscoveryService
import com.mpdc4gsr.libunified.app.messaging.ReliableMessageService
import com.mpdc4gsr.libunified.app.security.CertificateManager
import com.mpdc4gsr.libunified.app.sync.TimeSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.recording.session.SessionInfo
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket

class NetworkClient(private val context: Context) {
    companion object {
        private const val PC_CONTROLLER_PORT = 8080
        private const val DISCOVERY_PORT = 8081
        private const val BROADCAST_TIMEOUT = 5000L
        private const val CONNECTION_TIMEOUT = 10000L
        private const val QUERY_TIMEOUT = 2000
        private const val HEARTBEAT_INTERVAL = 5000L
        private const val DISCOVERY_WAIT_MS = 5000L
        private const val MAX_MESSAGE_SIZE = 1024 * 1024
    }


    private var socket: Socket? = null
    private var sslSocket: SSLSocket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isConnected = false
    private var isSecureConnection = false
    private var useSecureDefault = true
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

    // Stub: NetworkErrorRecoveryManager not available
    // private lateinit var errorRecoveryManager: NetworkErrorRecoveryManager
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
        // Stub: NetworkErrorRecoveryManager not available
        // errorRecoveryManager = NetworkErrorRecoveryManager(context, this)
        // setupErrorRecoveryListener()
    }


    fun initialize(): Boolean {
        certificateManager.initialize()
        discoveryService.setEventListener(
            object : NetworkDiscoveryService.DiscoveryEventListener {
                override fun onDeviceDiscovered(device: NetworkDiscoveryService.DiscoveredDevice) {
                    if (device.deviceType == NetworkDiscoveryService.DeviceType.PC_CONTROLLER) {
                        val controller =
                            ControllerInfo(
                                ipAddress = device.ipAddress,
                                port = device.port,
                                deviceName = device.serviceName,
                                capabilities = device.attributes.values.toList(),
                            )
                        discoveredControllers[device.ipAddress] = controller
                        eventListener?.onControllerDiscovered(controller)
                    }
                }


                override fun onDeviceLost(serviceName: String) {
                }


                override fun onDiscoveryStarted() {
                }


                override fun onDiscoveryStopped() {
                }


                override fun onError(
                    operation: String,
                    error: String,
                ) {
                    eventListener?.onError("discovery_$operation", error)
                }
            },
        )
        timeSyncService.setListener(
            object : TimeSyncService.TimeSyncListener {
                override fun onSyncCompleted(result: TimeSyncService.SyncResult) {
                    if (result.isSuccess) {
                        clockOffset = result.clockOffsetMs * 1_000_000
                        eventListener?.onTimeSynchronized(clockOffset)
                    }
                }


                override fun onSyncStarted(targetHost: String) {
                }


                override fun onSyncError(error: String) {
                    eventListener?.onError("time_sync", error)
                }
            },
        )
        reliableMessaging.setTransport(
            object : ReliableMessageService.MessageTransport {
                override suspend fun sendMessage(
                    host: String,
                    port: Int,
                    message: JSONObject,
                ): Boolean {
                    return try {
                        sendDirectMessage(message)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            },
        )
        reliableMessaging.initialize()
            return true
    }


    fun setEventListener(listener: NetworkEventListener?) {
        eventListener = listener
    }


    fun setMessageHandler(
        messageType: String,
        handler: (JSONObject) -> Unit,
    ) {
        messageHandlers[messageType] = handler
    }


    private fun setupErrorRecoveryListener() {
        // Stub: NetworkErrorRecoveryManager not available
    }


    suspend fun discoverControllers(): List<ControllerInfo> =
        withContext(Dispatchers.IO) {
            val controllers = mutableListOf<ControllerInfo>()
            discoveryService.startDiscovery()
            delay(DISCOVERY_WAIT_MS)
            val discoveredDevices =
                discoveryService.getDiscoveredDevicesByType(
                    NetworkDiscoveryService.DeviceType.PC_CONTROLLER,
                )
            discoveredDevices.forEach { device ->
                val controller =
                    ControllerInfo(
                        ipAddress = device.ipAddress,
                        port = device.port,
                        deviceName = device.serviceName,
                        capabilities = device.attributes.values.toList(),
                    )
                controllers.add(controller)
            }

            if (controllers.isEmpty()) {
                val scanResults = performSubnetScan()
                controllers.addAll(scanResults)
            }

            controllers
        }


    private suspend fun performSubnetScan(): List<ControllerInfo> =
        withContext(Dispatchers.IO) {
            val controllers = mutableListOf<ControllerInfo>()
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val dhcpInfo = wifiManager.dhcpInfo
            if (dhcpInfo.gateway == 0) {
                return@withContext controllers
            }

            val gateway = intToIp(dhcpInfo.gateway)
            val subnet = gateway.substring(0, gateway.lastIndexOf('.'))
            val jobs =
                (1..254).map { hostNum ->
                    async {
                        val host = "$subnet.$hostNum"
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
                    }
                }

            jobs.awaitAll().filterNotNull().forEach { controllers.add(it) }

            controllers
        }


    suspend fun connectToController(
        ipAddress: String,
        port: Int = PC_CONTROLLER_PORT,
        useSecure: Boolean = useSecureDefault,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (isConnected) {
                    disconnect()
                }

                if (useSecure) {
                    val sslContext = certificateManager.createSSLContext()
                    if (sslContext != null) {
                        TrafficStats.setThreadStatsTag(Process.myTid())
                        val sslSocketFactory = sslContext.socketFactory
                        sslSocket = sslSocketFactory.createSocket(ipAddress, port) as SSLSocket
                        sslSocket?.soTimeout = CONNECTION_TIMEOUT.toInt()
                        sslSocket?.let { TrafficStats.tagSocket(it as Socket) }

                        sslSocket?.startHandshake()
                        outputStream = DataOutputStream(sslSocket?.getOutputStream())
                        inputStream = DataInputStream(sslSocket?.getInputStream())
                        isSecureConnection = true
                    } else {
                        return@withContext connectPlaintext(ipAddress, port)
                    }
                } else {
                    return@withContext connectPlaintext(ipAddress, port)
                }

                isConnected = true
                startMessageListener()
                val registrationSuccess = registerDeviceSecure()
                if (registrationSuccess) {
                    val syncResult = timeSyncService.synchronizeTime(ipAddress, port)
                    if (syncResult.isSuccess) {
                        clockOffset = syncResult.clockOffsetMs * 1_000_000
                        timeSyncService.startPeriodicSync(ipAddress, port)
                    }

                    startHeartbeat()
                    val controller =
                        discoveredControllers[ipAddress]
                            ?: ControllerInfo(ipAddress, port, "PC Controller", listOf("recording"))
                    eventListener?.onConnected(controller)
                    true
                } else {
                    disconnect()
                    false
                }
            } catch (e: SSLException) {
                disconnect()
                connectPlaintext(ipAddress, port)
            } catch (e: Exception) {
                eventListener?.onError("connect", e.message ?: "Connection failed")
                disconnect()
                false
            }
        }


    private suspend fun connectPlaintext(
        ipAddress: String,
        port: Int,
    ): Boolean {
        return try {
            TrafficStats.setThreadStatsTag(Process.myTid())
            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT.toInt())
            newSocket.soTimeout = CONNECTION_TIMEOUT.toInt()
            TrafficStats.tagSocket(newSocket)
            socket = newSocket
            outputStream = DataOutputStream(socket?.getOutputStream())
            inputStream = DataInputStream(socket?.getInputStream())
            isSecureConnection = false
            isConnected = true
            startMessageListener()
            val registrationSuccess = registerDevice()
            if (registrationSuccess) {
                startHeartbeat()
                val controller =
                    discoveredControllers[ipAddress]
                        ?: ControllerInfo(ipAddress, port, "PC Controller", listOf("recording"))
                eventListener?.onConnected(controller)
                true
            } else {
                disconnect()
                false
            }
        } catch (e: Exception) {
            eventListener?.onError("connect", e.message ?: "Connection failed")
            disconnect()
            false
        }
    }


    fun disconnect() {
        isConnected = false
        heartbeatJob.cancel()
        timeSyncService.stopPeriodicSync()
        discoveryService.stopDiscovery()
        try {
            socket?.let { TrafficStats.untagSocket(it) }

            sslSocket?.let { TrafficStats.untagSocket(it) }

            outputStream?.close()
            inputStream?.close()
            sslSocket?.close()
            socket?.close()
            TrafficStats.clearThreadStatsTag()
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(
                "NetworkClient",
                "Failed to release network client resources",
                e,
            )
        } finally {
            outputStream = null
            inputStream = null
            sslSocket = null
            socket = null
            isSecureConnection = false
        }

        eventListener?.onDisconnected("User initiated")
    }


    suspend fun sendMeasurementData(
        sessionId: String,
        data: JSONObject,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected)
            return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "measurement_data")
                        put("device_id", deviceId)
                        put("session_id", sessionId)
                        put("timestamp", getSynchronizedTimestamp())
                        put("data", data)
                    }

                sendMessageInternal(message)
                true
            } catch (e: Exception) {
                eventListener?.onError("send_data", e.message ?: "Send failed")
                false
            }
        }


    suspend fun reportStatus(
        status: String,
        batteryLevel: Int? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected)
            return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "device_status")
                        put("device_id", deviceId)
                        put("status", status)
                        batteryLevel?.let { put("battery_level", it) }

                        put(
                            "timestamp",
                            getCurrentTimestamp()
                        )
                    }

                sendMessageInternal(message)
                true
            } catch (e: Exception) {
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

                sendMessageInternal(registrationMessage)
                val response = receiveMessage(5000)
                response?.optString("message_type") == "ack" &&
                        response.optString("ack_for") == "device_register"
            } catch (e: Exception) {
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
                        put(
                            "timestamp",
                            getCurrentTimestamp()
                        )
                    }

                sendMessageInternal(registrationMessage)
                val response = receiveMessage(5000)
                response?.optString("message_type") == "ack" &&
                        response.optString("ack_for") == "device_register"
            } catch (e: Exception) {
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

                    sendMessageInternal(heartbeatMessage)
                    delay(HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }


    private fun handleIncomingMessage(message: JSONObject) {
        val messageType = message.optString("message_type")
        messageHandlers[messageType]?.let { handler ->
            handler(message)
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
            }

            "ack" -> {
            }

            "error" -> {
                val errorMsg = message.optString("error_message", "Unknown error")
                eventListener?.onError("pc_controller", errorMsg)
            }

            else -> {
            }
        }
    }


    private suspend fun sendMessageInternal(message: JSONObject) =
        withContext(Dispatchers.IO) {
            val output = outputStream ?: throw IOException("Not connected")
            val messageData = message.toString().toByteArray(Charsets.UTF_8)
            val startTime = System.currentTimeMillis()
            output.writeInt(messageData.size)
            output.write(messageData)
            output.flush()
            // errorRecoveryManager.recordDataTransfer(messageData.size.toLong() + 4)
            if (message.optString("message_type") == "device_heartbeat") {
                val latency = System.currentTimeMillis() - startTime
                // errorRecoveryManager.recordLatency(latency)
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
                if (messageLength > MAX_MESSAGE_SIZE) {
                    throw IOException("Message too large: $messageLength bytes")
                }

                val messageData = ByteArray(messageLength)
                input.readFully(messageData)
                socketToUse?.soTimeout = originalTimeout ?: CONNECTION_TIMEOUT.toInt()
                JSONObject(String(messageData, Charsets.UTF_8))
            } catch (e: SocketTimeoutException) {
                null
            } catch (e: Exception) {
                throw e
            }
        }


    fun getSynchronizedTimestamp(): Long {
        return System.nanoTime() + clockOffset
    }


    suspend fun sendMessage(message: JSONObject): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!isConnected) {
                    return@withContext false
                }

                sendMessageInternal(message)
                true
            } catch (e: Exception) {
                false
            }
        }


    suspend fun startDataStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected)
            return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "start_data_stream")
                        put("device_id", deviceId)
                        put("timestamp", getSynchronizedTimestamp())
                    }

                sendMessageInternal(message)
                eventListener?.onDataStreamingStarted()
                true
            } catch (e: Exception) {
                false
            }
        }


    suspend fun stopDataStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConnected)
            return@withContext false
            try {
                val message =
                    JSONObject().apply {
                        put("message_type", "stop_data_stream")
                        put("device_id", deviceId)
                        put("timestamp", getSynchronizedTimestamp())
                    }

                sendMessageInternal(message)
                eventListener?.onDataStreamingStopped()
                true
            } catch (e: Exception) {
                false
            }
        }


    private suspend fun queryController(host: String): ControllerInfo? =
        withContext(Dispatchers.IO) {
            try {
                TrafficStats.setThreadStatsTag(Process.myTid())
                Socket().use { socket ->
                    TrafficStats.tagSocket(socket)
                    socket.connect(InetSocketAddress(host, PC_CONTROLLER_PORT), QUERY_TIMEOUT)
                    DataOutputStream(socket.getOutputStream()).use { output ->
                        DataInputStream(socket.getInputStream()).use { input ->
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
                            if (responseLength < 0 || responseLength > MAX_MESSAGE_SIZE) {
                                return@withContext null
                            }

                            val responseData = ByteArray(responseLength)
                            input.readFully(responseData)
                            val response = JSONObject(String(responseData, Charsets.UTF_8))
                            if (response.optString("message_type") == "info_response") {
                                ControllerInfo(
                                    ipAddress = host,
                                    port = PC_CONTROLLER_PORT,
                                    deviceName = response.optString("device_name", "PC Controller"),
                                    capabilities =
                                        response.optJSONArray("capabilities")?.let { jsonArray ->
                                            (0 until jsonArray.length()).map { jsonArray.getString(it) }
                                        } ?: emptyList(),
                                )
                            } else {
                                null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                null
            } finally {
                TrafficStats.clearThreadStatsTag()
            }
        }


    private suspend fun isHostReachable(
        host: String,
        port: Int,
        timeoutMs: Int,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                TrafficStats.setThreadStatsTag(Process.myTid())
                Socket().use { socket ->
                    TrafficStats.tagSocket(socket)
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            } finally {
                TrafficStats.clearThreadStatsTag()
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
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val dhcpInfo = wifiManager.dhcpInfo
            return intToIp(dhcpInfo.ipAddress)
        } catch (e: Exception) {
            return "127.0.0.1"
        }
    }


    fun getDiscoveredControllers(): List<ControllerInfo> = discoveredControllers.values.toList()
    private suspend fun sendDirectMessage(message: JSONObject) =
        withContext(Dispatchers.IO) {
            val output = outputStream ?: throw IOException("Not connected")
            val messageData = message.toString().toByteArray(Charsets.UTF_8)
            output.writeInt(messageData.size)
            output.write(messageData)
            output.flush()
        }


    fun isSecureConnection(): Boolean = isSecureConnection
    fun isConnected(): Boolean = isConnected
    fun setSecureConnectionDefault(enabled: Boolean) {
        if (isConnected) {
            return
        }

        useSecureDefault = enabled
    }

    // Stub: NetworkErrorRecoveryManager not available
    // fun getErrorRecoveryManager(): NetworkErrorRecoveryManager = errorRecoveryManager
    fun startDiscovery(callback: (Boolean) -> Unit) {
        heartbeatScope.launch {
            try {
                discoverControllers()
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }


    fun connectToController(
        address: String,
        port: Int,
        callback: (Boolean) -> Unit,
    ) {
        heartbeatScope.launch {
            try {
                val result = connectToController(address, port, useSecureDefault)
                callback(result)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }


    fun getLatencyMs(): Long {
        return 0L // Stub: errorRecoveryManager not available
    }


    fun getThroughputKBps(): Double {
        return 0.0 // Stub: errorRecoveryManager not available
    }


    fun cleanup() {
        disconnect()
        discoveryService.cleanup()
        timeSyncService.cleanup()
        reliableMessaging.shutdown()
        // errorRecoveryManager.cleanup()
        discoveredControllers.clear()
        eventListener = null
    }
}

