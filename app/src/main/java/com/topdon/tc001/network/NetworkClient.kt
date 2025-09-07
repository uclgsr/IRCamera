package com.topdon.tc001.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.topdon.gsr.model.SessionInfo
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.*
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
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isConnected = false
    private var deviceId: String = android.provider.Settings.Secure.getString(
        context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
    )
    
    private val heartbeatJob = SupervisorJob()
    private val heartbeatScope = CoroutineScope(Dispatchers.IO + heartbeatJob)
    
    private val messageHandlers = ConcurrentHashMap<String, (JSONObject) -> Unit>()
    private val discoveredControllers = ConcurrentHashMap<String, ControllerInfo>()
    
    data class ControllerInfo(
        val ipAddress: String,
        val port: Int,
        val deviceName: String,
        val capabilities: List<String>,
        val lastSeen: Long = System.currentTimeMillis()
    )
    
    interface NetworkEventListener {
        fun onControllerDiscovered(controller: ControllerInfo)
        fun onConnected(controller: ControllerInfo)
        fun onDisconnected(reason: String)
        fun onRemoteMeasurementRequest(sessionInfo: SessionInfo)
        fun onSyncFlash(durationMs: Int)
        fun onError(operation: String, error: String)
    }
    
    private var eventListener: NetworkEventListener? = null
    
    fun setEventListener(listener: NetworkEventListener?) {
        eventListener = listener
    }
    
    /**
     * Discover PC Controllers on the same network
     */
    suspend fun discoverControllers(): List<ControllerInfo> = withContext(Dispatchers.IO) {
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
            val jobs = (1..254).map { hostNum ->
                async {
                    val host = "$subnet.$hostNum"
                    try {
                        if (isHostReachable(host, PC_CONTROLLER_PORT, 1000)) {
                            val controller = queryController(host)
                            if (controller != null) {
                                discoveredControllers[host] = controller
                                eventListener?.onControllerDiscovered(controller)
                                controller
                            } else null
                        } else null
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
     * Connect to a specific PC Controller
     */
    suspend fun connectToController(ipAddress: String, port: Int = PC_CONTROLLER_PORT): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                if (isConnected) {
                    disconnect()
                }
                
                Log.i(TAG, "Connecting to PC Controller at $ipAddress:$port")
                
                // Create regular socket first (remove TLS for simplicity)
                socket = Socket()
                socket?.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT.toInt())
                socket?.soTimeout = CONNECTION_TIMEOUT.toInt()
                
                outputStream = DataOutputStream(socket?.getOutputStream())
                inputStream = DataInputStream(socket?.getInputStream())
                
                isConnected = true
                
                // Start message listening
                startMessageListener()
                
                // Send device registration
                val registrationSuccess = registerDevice()
                
                if (registrationSuccess) {
                    // Start heartbeat
                    startHeartbeat()
                    
                    val controller = discoveredControllers[ipAddress] 
                        ?: ControllerInfo(ipAddress, port, "PC Controller", listOf("recording"))
                    eventListener?.onConnected(controller)
                    
                    Log.i(TAG, "Successfully connected and registered with PC Controller")
                    true
                } else {
                    disconnect()
                    false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to PC Controller", e)
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
        
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            outputStream = null
            inputStream = null
            socket = null
        }
        
        eventListener?.onDisconnected("User initiated")
        Log.i(TAG, "Disconnected from PC Controller")
    }
    
    /**
     * Send measurement data to PC Controller
     */
    suspend fun sendMeasurementData(sessionId: String, data: JSONObject): Boolean = 
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false
            
            try {
                val message = JSONObject().apply {
                    put("message_type", "measurement_data")
                    put("device_id", deviceId)
                    put("session_id", sessionId)
                    put("timestamp", System.currentTimeMillis())
                    put("data", data)
                }
                
                sendMessage(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send measurement data", e)
                eventListener?.onError("send_data", e.message ?: "Send failed")
                false
            }
        }
    
    /**
     * Report device status to PC Controller
     */
    suspend fun reportStatus(status: String, batteryLevel: Int? = null): Boolean = 
        withContext(Dispatchers.IO) {
            if (!isConnected) return@withContext false
            
            try {
                val message = JSONObject().apply {
                    put("message_type", "device_status")
                    put("device_id", deviceId)
                    put("status", status)
                    batteryLevel?.let { put("battery_level", it) }
                    put("timestamp", System.currentTimeMillis())
                }
                
                sendMessage(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report status", e)
                false
            }
        }
    
    private suspend fun registerDevice(): Boolean = withContext(Dispatchers.IO) {
        try {
            val capabilities = listOf(
                "gsr_sensor",
                "thermal_camera", 
                "rgb_camera",
                "raw_capture",
                "video_recording",
                "bluetooth_sync"
            )
            
            val registrationMessage = JSONObject().apply {
                put("message_type", "device_register")
                put("device_id", deviceId)
                put("device_type", "MPDC4GSR_Mobile")
                put("capabilities", capabilities.joinToString(","))
                put("timestamp", System.currentTimeMillis())
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
                    val heartbeatMessage = JSONObject().apply {
                        put("message_type", "device_heartbeat")
                        put("device_id", deviceId)
                        put("timestamp", System.currentTimeMillis())
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
                
                val sessionInfo = SessionInfo(
                    sessionId = sessionId,
                    sessionName = sessionName,
                    startTime = System.currentTimeMillis(),
                    participantId = "remote",
                    notes = "PC Controller initiated session"
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
    
    private suspend fun sendMessage(message: JSONObject) = withContext(Dispatchers.IO) {
        val output = outputStream ?: throw IOException("Not connected")
        
        val messageData = message.toString().toByteArray(Charsets.UTF_8)
        output.writeInt(messageData.size)
        output.write(messageData)
        output.flush()
    }
    
    private suspend fun receiveMessage(timeoutMs: Long): JSONObject? = withContext(Dispatchers.IO) {
        val input = inputStream ?: return@withContext null
        
        try {
            val originalTimeout = socket?.soTimeout
            socket?.soTimeout = timeoutMs.toInt()
            
            val messageLength = input.readInt()
            if (messageLength > 1024 * 1024) { // 1MB limit
                throw IOException("Message too large: $messageLength bytes")
            }
            
            val messageData = ByteArray(messageLength)
            input.readFully(messageData)
            
            socket?.soTimeout = originalTimeout ?: CONNECTION_TIMEOUT.toInt()
            
            JSONObject(String(messageData, Charsets.UTF_8))
            
        } catch (e: SocketTimeoutException) {
            null // Normal timeout, not an error
        } catch (e: Exception) {
            throw e
        }
    }
    
    private suspend fun queryController(host: String): ControllerInfo? = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, PC_CONTROLLER_PORT), 2000)
            
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())
            
            // Send info query
            val query = JSONObject().apply {
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
                    capabilities = response.optString("capabilities", "").split(",")
                )
            } else null
            
        } catch (e: Exception) {
            Log.d(TAG, "Controller query failed for $host: ${e.message}")
            null
        }
    }
    
    private suspend fun isHostReachable(host: String, port: Int, timeoutMs: Int): Boolean = 
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
        return ((ipAddress and 0xFF).toString() + "." +
                ((ipAddress shr 8) and 0xFF).toString() + "." +
                ((ipAddress shr 16) and 0xFF).toString() + "." +
                ((ipAddress shr 24) and 0xFF).toString())
    }
    
    fun isConnected(): Boolean = isConnected
    
    fun getDiscoveredControllers(): List<ControllerInfo> = discoveredControllers.values.toList()
}