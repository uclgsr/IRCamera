package com.topdon.tc001.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.topdon.tc001.config.FeatureFlags
import com.topdon.tc001.config.ProtocolVersion
import com.topdon.tc001.logging.StructuredLogger
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.internal.ws.RealWebSocket
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlin.random.Random
import android.util.Base64
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * WebSocket client for PC-to-phone communication
 * Phase 1 implementation - Android as WebSocket client connecting to PC WSS server
 *
 * Features:
 * - WebSocket Secure (WSS) connections with TLS
 * - mDNS/Zeroconf discovery of PC controllers
 * - Basic authentication (admin/admin)
 * - Auto-reconnection with exponential backoff (1-8s with jitter)
 * - WebSocket heartbeat (PING every 5s, disconnect on 15s silence)
 * - Persistent connection across network interruptions
 */
class WebSocketClient(private val context: Context) {
    
    companion object {
        private const val TAG = "WebSocketClient"
        
        // Connection configuration
        private const val DEFAULT_PC_PORT = 8443  // WSS port
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val READ_TIMEOUT_MS = 30000L
        private const val WRITE_TIMEOUT_MS = 10000L
        
        // Heartbeat configuration
        private const val HEARTBEAT_INTERVAL_MS = 5000L  // 5 seconds
        private const val HEARTBEAT_TIMEOUT_MS = 15000L  // 15 seconds silence = disconnect
        
        // Auto-reconnection configuration
        private const val RECONNECT_BASE_DELAY_MS = 1000L  // 1 second base
        private const val RECONNECT_MAX_DELAY_MS = 8000L   // 8 seconds max
        private const val RECONNECT_JITTER_MS = 500L       // ±500ms jitter
        
        // Discovery configuration
        private const val SERVICE_TYPE = "_irhub._tcp."
        private const val DISCOVERY_TIMEOUT_MS = 10000L
        
        // Authentication
        private const val AUTH_USERNAME = "admin"
        private const val AUTH_PASSWORD = "admin"
    }
    
    // Connection state
    private val isConnected = AtomicBoolean(false)
    private val isAuthenticating = AtomicBoolean(false)
    private val isAuthenticated = AtomicBoolean(false)
    private val isReconnecting = AtomicBoolean(false)
    
    // WebSocket components  
    private val okHttpClient: OkHttpClient
    private val webSocket = AtomicReference<WebSocket?>()
    private val currentServerInfo = AtomicReference<ServerInfo?>()
    
    // Coroutine management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var discoveryJob: Job? = null
    
    // Services
    private val logger = StructuredLogger.getInstance(context)
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    
    // Event listeners
    private var eventListener: WebSocketEventListener? = null
    private val discoveredServers = mutableMapOf<String, ServerInfo>()
    
    // Metrics
    private var connectionAttempts = 0
    private var lastHeartbeatTime = 0L
    private var connectionStartTime = 0L
    
    data class ServerInfo(
        val name: String,
        val host: String,
        val port: Int,
        val usesTLS: Boolean,
        val protocolVersion: String,
        val capabilities: Set<String>
    )
    
    interface WebSocketEventListener {
        fun onConnecting(serverInfo: ServerInfo)
        fun onConnected(serverInfo: ServerInfo)
        fun onAuthenticated()
        fun onDisconnected(reason: String)
        fun onMessage(messageType: String, message: JSONObject)
        fun onError(error: String, exception: Throwable?)
        fun onServerDiscovered(serverInfo: ServerInfo)
        fun onHeartbeatReceived()
    }
    
    init {
        // Initialize OkHttp client with WebSocket support
        okHttpClient = createOkHttpClient()
        
        // Log initialization
        logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "initialized", mapOf(
            "device_id" to getDeviceId(),
            "tls_enabled" to FeatureFlags.TLS_ENABLE,
            "mdns_enabled" to FeatureFlags.MDNS_ENABLE
        ))
    }
    
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
        
        // Add TLS configuration if enabled
        if (FeatureFlags.TLS_ENABLE) {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }  // Accept all hostnames for development
        }
        
        return builder.build()
    }
    
    /**
     * Set event listener for WebSocket events
     */
    fun setEventListener(listener: WebSocketEventListener) {
        this.eventListener = listener
    }
    
    /**
     * Start discovery and connection process
     */
    fun start() {
        if (isConnected.get()) {
            Log.w(TAG, "WebSocket client already connected")
            return
        }
        
        Log.i(TAG, "Starting WebSocket client")
        logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "start_requested", emptyMap())
        
        // Start server discovery
        startServerDiscovery()
    }
    
    /**
     * Stop WebSocket client and cleanup resources
     */
    fun stop() {
        Log.i(TAG, "Stopping WebSocket client")
        logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "stop_requested", emptyMap())
        
        // Cancel all jobs
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        discoveryJob?.cancel()
        
        // Close WebSocket connection
        webSocket.get()?.close(1000, "Client stopping")
        webSocket.set(null)
        
        // Reset state
        isConnected.set(false)
        isAuthenticated.set(false)
        isReconnecting.set(false)
        currentServerInfo.set(null)
        
        // Stop NSD discovery
        stopServerDiscovery()
        
        eventListener?.onDisconnected("Client stopped")
    }
    
    /**
     * Start mDNS/NSD server discovery
     */
    private fun startServerDiscovery() {
        if (!FeatureFlags.MDNS_ENABLE) {
            Log.w(TAG, "mDNS discovery disabled, trying manual connection")
            // TODO: Try manual connection to common IP addresses
            return
        }
        
        discoveryJob = scope.launch {
            try {
                Log.i(TAG, "Starting NSD discovery for $SERVICE_TYPE")
                logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "discovery_started", mapOf(
                    "service_type" to SERVICE_TYPE
                ))
                
                val discoveryListener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.e(TAG, "Discovery start failed: $errorCode")
                        logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "discovery_start_failed", mapOf(
                            "error_code" to errorCode
                        ))
                    }
                    
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.e(TAG, "Discovery stop failed: $errorCode")
                    }
                    
                    override fun onDiscoveryStarted(serviceType: String) {
                        Log.i(TAG, "Discovery started for $serviceType")
                    }
                    
                    override fun onDiscoveryStopped(serviceType: String) {
                        Log.i(TAG, "Discovery stopped for $serviceType")
                    }
                    
                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        Log.i(TAG, "Service found: ${serviceInfo.serviceName}")
                        resolveService(serviceInfo)
                    }
                    
                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                        Log.i(TAG, "Service lost: ${serviceInfo.serviceName}")
                        discoveredServers.remove(serviceInfo.serviceName)
                    }
                }
                
                nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                
                // Timeout discovery after specified time
                delay(DISCOVERY_TIMEOUT_MS)
                
                // If no servers discovered, try manual connection
                if (discoveredServers.isEmpty()) {
                    Log.w(TAG, "No servers discovered via mDNS, trying manual connection")
                    tryManualConnection()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in server discovery", e)
                logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "discovery_error", mapOf(
                    "error" to e.message
                ))
            }
        }
    }
    
    /**
     * Resolve discovered NSD service
     */
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }
            
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Service resolved: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}")
                
                // Extract service properties
                val attributes = serviceInfo.attributes
                val protocolVersion = String(attributes["proto"] ?: "v1".toByteArray())
                val usesTLS = String(attributes["tls"] ?: "1".toByteArray()) == "1"
                val capabilities = String(attributes["capabilities"] ?: "".toByteArray()).split(",").toSet()
                
                val serverInfo = ServerInfo(
                    name = serviceInfo.serviceName,
                    host = serviceInfo.host.hostAddress ?: "unknown",
                    port = serviceInfo.port,
                    usesTLS = usesTLS,
                    protocolVersion = protocolVersion,
                    capabilities = capabilities
                )
                
                discoveredServers[serviceInfo.serviceName] = serverInfo
                eventListener?.onServerDiscovered(serverInfo)
                
                // Auto-connect to first discovered server if not already connected
                if (!isConnected.get() && !isReconnecting.get()) {
                    connectToServer(serverInfo)
                }
            }
        }
        
        nsdManager.resolveService(serviceInfo, resolveListener)
    }
    
    /**
     * Try manual connection to common IP addresses
     */
    private fun tryManualConnection() {
        val commonAddresses = listOf(
            "192.168.1.1", "192.168.0.1", "192.168.1.100", "192.168.0.100",
            "10.0.0.1", "172.16.0.1", "localhost", "127.0.0.1"
        )
        
        scope.launch {
            for (address in commonAddresses) {
                if (isConnected.get()) break
                
                val serverInfo = ServerInfo(
                    name = "Manual-$address",
                    host = address,
                    port = DEFAULT_PC_PORT,
                    usesTLS = FeatureFlags.TLS_ENABLE,
                    protocolVersion = "v1",
                    capabilities = emptySet()
                )
                
                Log.i(TAG, "Trying manual connection to $address:$DEFAULT_PC_PORT")
                connectToServer(serverInfo)
                
                delay(2000) // Wait 2s before trying next address
            }
        }
    }
    
    /**
     * Connect to a specific server
     */
    private fun connectToServer(serverInfo: ServerInfo) {
        if (isConnected.get()) {
            Log.w(TAG, "Already connected")
            return
        }
        
        currentServerInfo.set(serverInfo)
        connectionStartTime = System.currentTimeMillis()
        connectionAttempts++
        
        Log.i(TAG, "Connecting to ${serverInfo.name} at ${serverInfo.host}:${serverInfo.port}")
        logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "connection_attempt", mapOf(
            "server_name" to serverInfo.name,
            "host" to serverInfo.host,
            "port" to serverInfo.port,
            "attempt" to connectionAttempts
        ))
        
        eventListener?.onConnecting(serverInfo)
        
        // Build WebSocket URL
        val protocol = if (serverInfo.usesTLS) "wss" else "ws"
        val url = "$protocol://${serverInfo.host}:${serverInfo.port}/"
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connection opened")
                isConnected.set(true)
                this@WebSocketClient.webSocket.set(webSocket)
                
                logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "connection_opened", mapOf(
                    "server_name" to serverInfo.name,
                    "response_code" to response.code
                ))
                
                eventListener?.onConnected(serverInfo)
                
                // Start protocol handshake
                scope.launch {
                    performHandshake()
                }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleMessage(text)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket connection closing: $code $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket connection closed: $code $reason")
                handleDisconnection("Connection closed: $reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failed", t)
                handleDisconnection("Connection failed: ${t.message}")
            }
        }
        
        okHttpClient.newWebSocket(request, webSocketListener)
    }
    
    /**
     * Perform protocol handshake
     */
    private suspend fun performHandshake() {
        try {
            val handshakeMessage = ProtocolVersion.createHandshakeMessage(getDeviceId())
            sendMessage(handshakeMessage)
            
            Log.i(TAG, "Protocol handshake sent")
            logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "handshake_sent", emptyMap())
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing handshake", e)
            logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "handshake_error", mapOf(
                "error" to e.message
            ))
        }
    }
    
    /**
     * Perform authentication
     */
    private suspend fun performAuthentication() {
        try {
            isAuthenticating.set(true)
            
            val credentials = "$AUTH_USERNAME:$AUTH_PASSWORD"
            val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            
            val authMessage = ProtocolVersion.createProtocolMessage("auth_request", JSONObject().apply {
                put("auth_type", "basic")
                put("credentials", encodedCredentials)
            })
            
            sendMessage(authMessage)
            
            Log.i(TAG, "Authentication request sent")
            logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "auth_sent", emptyMap())
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing authentication", e)
            isAuthenticating.set(false)
            logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "auth_error", mapOf(
                "error" to e.message
            ))
        }
    }
    
    /**
     * Handle incoming WebSocket message
     */
    private suspend fun handleMessage(text: String) {
        try {
            val message = JSONObject(text)
            val messageType = message.optString("message_type", "")
            
            // Validate protocol version
            if (!ProtocolVersion.validateMessageVersion(message)) {
                Log.w(TAG, "Received message with invalid protocol version")
                return
            }
            
            Log.d(TAG, "Received message: $messageType")
            
            when (messageType) {
                "protocol_handshake_response" -> handleHandshakeResponse(message)
                "auth_response" -> handleAuthResponse(message)
                "ping" -> handlePing(message)
                "heartbeat_response" -> handleHeartbeatResponse(message)
                "sync_flash_trigger" -> handleSyncFlash(message)
                "session_start_response" -> handleSessionResponse(message)
                "session_stop_response" -> handleSessionResponse(message)
                "error" -> handleError(message)
                else -> {
                    Log.w(TAG, "Unknown message type: $messageType")
                    eventListener?.onMessage(messageType, message)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
            logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "message_error", mapOf(
                "error" to e.message
            ))
        }
    }
    
    /**
     * Handle protocol handshake response
     */
    private suspend fun handleHandshakeResponse(message: JSONObject) {
        val authRequired = message.optBoolean("auth_required", false)
        
        if (authRequired) {
            performAuthentication()
        } else {
            // No auth required, start heartbeat
            startHeartbeat()
        }
    }
    
    /**
     * Handle authentication response
     */
    private suspend fun handleAuthResponse(message: JSONObject) {
        isAuthenticating.set(false)
        val success = message.optBoolean("success", false)
        
        if (success) {
            Log.i(TAG, "Authentication successful")
            isAuthenticated.set(true)
            logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "auth_success", emptyMap())
            
            eventListener?.onAuthenticated()
            startHeartbeat()
        } else {
            val error = message.optString("error_message", "Authentication failed")
            Log.e(TAG, "Authentication failed: $error")
            logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "auth_failed", mapOf(
                "error" to error
            ))
            
            // Disconnect on auth failure
            webSocket.get()?.close(4001, "Authentication failed")
        }
    }
    
    /**
     * Handle ping from server
     */
    private suspend fun handlePing(message: JSONObject) {
        lastHeartbeatTime = System.currentTimeMillis()
        
        // Send pong response
        val pongMessage = ProtocolVersion.createProtocolMessage("pong", JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
        })
        sendMessage(pongMessage)
    }
    
    /**
     * Handle heartbeat response
     */
    private suspend fun handleHeartbeatResponse(message: JSONObject) {
        lastHeartbeatTime = System.currentTimeMillis()
        eventListener?.onHeartbeatReceived()
    }
    
    /**
     * Handle sync flash trigger
     */
    private suspend fun handleSyncFlash(message: JSONObject) {
        eventListener?.onMessage("sync_flash", message)
    }
    
    /**
     * Handle session response
     */
    private suspend fun handleSessionResponse(message: JSONObject) {
        eventListener?.onMessage(message.optString("message_type"), message)
    }
    
    /**
     * Handle error message
     */
    private suspend fun handleError(message: JSONObject) {
        val errorType = message.optString("error_type", "unknown")
        val errorMessage = message.optString("error_message", "Unknown error")
        
        Log.e(TAG, "Server error: $errorType - $errorMessage")
        logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "server_error", mapOf(
            "error_type" to errorType,
            "error_message" to errorMessage
        ))
        
        eventListener?.onError("Server error: $errorMessage", null)
    }
    
    /**
     * Start heartbeat monitoring
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            lastHeartbeatTime = System.currentTimeMillis()
            
            while (isConnected.get() && isAuthenticated.get()) {
                try {
                    // Check for heartbeat timeout
                    val currentTime = System.currentTimeMillis()
                    if (lastHeartbeatTime > 0 && (currentTime - lastHeartbeatTime) > HEARTBEAT_TIMEOUT_MS) {
                        Log.w(TAG, "Heartbeat timeout, disconnecting")
                        webSocket.get()?.close(4000, "Heartbeat timeout")
                        break
                    }
                    
                    // Send heartbeat
                    val heartbeatMessage = ProtocolVersion.createProtocolMessage("heartbeat", JSONObject().apply {
                        put("timestamp", currentTime)
                    })
                    sendMessage(heartbeatMessage)
                    
                    delay(HEARTBEAT_INTERVAL_MS)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in heartbeat", e)
                    break
                }
            }
        }
    }
    
    /**
     * Handle disconnection and start reconnection if needed
     */
    private fun handleDisconnection(reason: String) {
        isConnected.set(false)
        isAuthenticated.set(false)
        webSocket.set(null)
        
        heartbeatJob?.cancel()
        
        logger.log(StructuredLogger.LogLevel.WARNING, "WebSocketClient", "disconnected", mapOf(
            "reason" to reason
        ))
        
        eventListener?.onDisconnected(reason)
        
        // Start auto-reconnection if not manually stopped
        if (!reason.contains("Client stopping")) {
            startReconnection()
        }
    }
    
    /**
     * Start auto-reconnection with exponential backoff
     */
    private fun startReconnection() {
        if (isReconnecting.get()) return
        
        isReconnecting.set(true)
        
        reconnectJob = scope.launch {
            var attempt = 1
            
            while (!isConnected.get() && isReconnecting.get()) {
                try {
                    // Calculate delay with exponential backoff and jitter
                    val baseDelay = min(RECONNECT_BASE_DELAY_MS * (1L shl (attempt - 1)), RECONNECT_MAX_DELAY_MS)
                    val jitter = Random.nextLong(-RECONNECT_JITTER_MS, RECONNECT_JITTER_MS)
                    val delay = baseDelay + jitter
                    
                    Log.i(TAG, "Reconnection attempt $attempt in ${delay}ms")
                    logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "reconnect_attempt", mapOf(
                        "attempt" to attempt,
                        "delay_ms" to delay
                    ))
                    
                    delay(delay)
                    
                    // Try to reconnect to last known server
                    currentServerInfo.get()?.let { serverInfo ->
                        connectToServer(serverInfo)
                    } ?: run {
                        // Restart discovery if no known server
                        startServerDiscovery()
                    }
                    
                    attempt++
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in reconnection", e)
                    break
                }
            }
            
            isReconnecting.set(false)
        }
    }
    
    /**
     * Stop server discovery
     */
    private fun stopServerDiscovery() {
        try {
            // Note: We don't keep a reference to the discovery listener,
            // so we can't explicitly stop it. This is a limitation of the current implementation.
            discoveryJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server discovery", e)
        }
    }
    
    /**
     * Send message to server
     */
    suspend fun sendMessage(message: JSONObject) {
        try {
            val webSocket = this.webSocket.get()
            if (webSocket == null) {
                Log.w(TAG, "Cannot send message - not connected")
                return
            }
            
            val jsonString = message.toString()
            val success = webSocket.send(jsonString)
            
            if (!success) {
                Log.w(TAG, "Failed to send message")
                logger.log(StructuredLogger.LogLevel.WARNING, "WebSocketClient", "send_failed", mapOf(
                    "message_type" to message.optString("message_type", "unknown")
                ))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "send_error", mapOf(
                "error" to e.message
            ))
        }
    }
    
    /**
     * Send session start request
     */
    suspend fun sendSessionStart(sessionId: String = "") {
        val message = ProtocolVersion.createProtocolMessage("session_start", JSONObject().apply {
            put("session_id", sessionId.ifEmpty { java.util.UUID.randomUUID().toString() })
            put("device_id", getDeviceId())
        })
        sendMessage(message)
    }
    
    /**
     * Send session stop request
     */
    suspend fun sendSessionStop(sessionId: String = "") {
        val message = ProtocolVersion.createProtocolMessage("session_stop", JSONObject().apply {
            put("session_id", sessionId)
            put("device_id", getDeviceId())
        })
        sendMessage(message)
    }
    
    /**
     * Send status request
     */
    suspend fun sendStatusRequest() {
        val message = ProtocolVersion.createProtocolMessage("status_request", JSONObject().apply {
            put("device_id", getDeviceId())
        })
        sendMessage(message)
    }
    
    /**
     * Get connection status
     */
    fun isConnected(): Boolean = isConnected.get()
    fun isAuthenticated(): Boolean = isAuthenticated.get()
    fun isReconnecting(): Boolean = isReconnecting.get()
    
    /**
     * Get current server info
     */
    fun getCurrentServer(): ServerInfo? = currentServerInfo.get()
    
    /**
     * Get discovered servers
     */
    fun getDiscoveredServers(): Map<String, ServerInfo> = discoveredServers.toMap()
    
    /**
     * Get device ID
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
}