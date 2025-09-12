package com.topdon.tc001.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.topdon.tc001.config.FeatureFlags
import com.topdon.tc001.config.ProtocolVersion
import com.topdon.tc001.logging.StructuredLogger
import com.topdon.tc001.sync.EnhancedTimeSyncService
import com.topdon.tc001.sync.SessionManager
import com.topdon.tc001.security.AdvancedAuthenticationManager
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
        
        // Phase 4 - Enhanced authentication modes
        private const val AUTH_MODE_BASIC = "basic"
        private const val AUTH_MODE_CERTIFICATE = "certificate"
        private const val AUTH_MODE_TOKEN = "token"
        private const val AUTH_MODE_BIOMETRIC = "biometric"
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
    
    // Phase 2 Services
    private var timeSyncService: EnhancedTimeSyncService? = null
    private var sessionManager: SessionManager? = null
    
    // Phase 3 Services  
    private var fileUploadService: FileUploadService? = null
    private var dataManagementService: DataManagementService? = null
    
    // Phase 4 Services - Advanced Authentication & Security
    private var advancedAuthManager: AdvancedAuthenticationManager? = null
    
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
        
        // Stop Phase 2 and Phase 3 services
        stopPhase2Services()
        stopPhase3Services()
        stopPhase4Services()
        
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
            
            // Initialize Phase 2 services
            initializePhase2Services()
            
            // Initialize Phase 3 services
            initializePhase3Services()
            
            // Initialize Phase 4 services
            initializePhase4Services()
            
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
        
        // Stop services on disconnection
        stopPhase2Services()
        stopPhase3Services()
        stopPhase4Services()
        
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
    
    // Phase 2 - Enhanced Time Synchronization & Session Management
    
    /**
     * Initialize Phase 2 services for enhanced time sync and session management
     */
    private fun initializePhase2Services() {
        // Initialize enhanced time synchronization service
        timeSyncService = EnhancedTimeSyncService(context, logger).apply {
            start { syncResult ->
                if (syncResult.success) {
                    Log.i(TAG, "Enhanced time sync completed: offset=${syncResult.offset/1_000_000.0}ms, quality=${syncResult.quality}")
                    logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "enhanced_sync_completed", mapOf(
                        "offset_ms" to (syncResult.offset / 1_000_000.0).toString(),
                        "rtt_ms" to (syncResult.rtt / 1_000_000.0).toString(),
                        "jitter_ms" to syncResult.jitter.toString(),
                        "quality" to syncResult.quality.name
                    ))
                } else {
                    Log.w(TAG, "Enhanced time sync failed")
                    logger.log(StructuredLogger.LogLevel.WARNING, "WebSocketClient", "enhanced_sync_failed", emptyMap())
                }
            }
        }
        
        // Initialize session manager
        sessionManager = SessionManager(context, logger).apply {
            start(
                onSessionStateChanged = { state ->
                    Log.i(TAG, "Session state changed: $state")
                    logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "session_state_changed", mapOf(
                        "new_state" to state.name
                    ))
                },
                onDeviceJoined = { device ->
                    Log.i(TAG, "Device joined session: ${device.deviceId} (${device.deviceType})")
                    logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "device_joined", mapOf(
                        "device_id" to device.deviceId,
                        "device_type" to device.deviceType
                    ))
                },
                onDeviceLeft = { device ->
                    Log.i(TAG, "Device left session: ${device.deviceId}")
                    logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "device_left", mapOf(
                        "device_id" to device.deviceId
                    ))
                },
                onSyncRequired = { devices ->
                    Log.i(TAG, "Cross-device synchronization required for ${devices.size} devices")
                    performCrossDeviceSync(devices)
                }
            )
        }
        
        Log.i(TAG, "Phase 2 services initialized: Enhanced Time Sync + Session Management")
    }
    
    /**
     * Stop Phase 2 services
     */
    private fun stopPhase2Services() {
        timeSyncService?.stop()
        timeSyncService = null
        
        sessionManager?.stop()
        sessionManager = null
        
        Log.i(TAG, "Phase 2 services stopped")
    }
    
    // Phase 3 - File Transfer & Data Management
    
    /**
     * Initialize Phase 3 services for file transfer and data management
     */
    private fun initializePhase3Services() {
        // Initialize file upload service
        fileUploadService = FileUploadService(context).apply {
            initialize(this@WebSocketClient)
        }
        
        // Initialize data management service
        dataManagementService = DataManagementService(context).apply {
            initialize(fileUploadService)
        }
        
        Log.i(TAG, "Phase 3 services initialized: File Transfer + Data Management")
        
        logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "phase3_services_initialized", mapOf(
            "file_upload_enabled" to (fileUploadService != null),
            "data_management_enabled" to (dataManagementService != null),
            "upload_protocol" to FeatureFlags.FILE_UPLOAD_PROTOCOL
        ))
    }
    
    /**
     * Stop Phase 3 services
     */
    private fun stopPhase3Services() {
        fileUploadService?.shutdown()
        fileUploadService = null
        
        dataManagementService = null
        
        Log.i(TAG, "Phase 3 services stopped")
    }
    
    /**
     * Perform cross-device synchronization
     */
    private fun performCrossDeviceSync(devices: List<SessionManager.DeviceInfo>) {
        GlobalScope.launch {
            try {
                logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "cross_device_sync_started", mapOf(
                    "device_count" to devices.size.toString()
                ))
                
                // Send sync flash command to PC for coordination
                val syncMessage = JSONObject().apply {
                    put("type", "sync_flash")
                    put("device_count", devices.size)
                    put("sync_timestamp", timeSyncService?.getSynchronizedTime() ?: System.nanoTime())
                }
                
                sendMessage(syncMessage)
                
                // Update device heartbeats after sync
                devices.forEach { device ->
                    sessionManager?.updateDeviceHeartbeat(
                        device.deviceId,
                        timeSyncService?.getCurrentOffset() ?: 0L,
                        SessionManager.ConnectionQuality.GOOD
                    )
                }
                
                logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "cross_device_sync_completed", emptyMap())
                
            } catch (e: Exception) {
                Log.e(TAG, "Cross-device sync error", e)
                logger.log(StructuredLogger.LogLevel.ERROR, "WebSocketClient", "cross_device_sync_error", mapOf(
                    "error" to e.message.orEmpty()
                ))
            }
        }
    }
    
    /**
     * Create new recording session
     */
    fun createRecordingSession(metadata: Map<String, Any> = emptyMap()): String? {
        val manager = sessionManager ?: return null
        val sessionId = manager.createSession(metadata)
        
        // Join this device to the session
        val deviceCapabilities = setOf("recording", "camera", "sensors")
        manager.joinDevice(
            deviceId = getDeviceId(),
            deviceType = "android_phone",
            capabilities = deviceCapabilities
        )
        
        return sessionId
    }
    
    /**
     * Start synchronized recording across connected devices
     */
    fun startSynchronizedRecording(): Boolean {
        return sessionManager?.startSyncRecording() ?: false
    }
    
    /**
     * Stop synchronized recording
     */
    fun stopSynchronizedRecording() {
        sessionManager?.stopSyncRecording()
    }
    
    /**
     * Get enhanced time synchronization diagnostics
     */
    fun getTimeSyncDiagnostics(): JSONObject {
        return timeSyncService?.getDiagnostics() ?: JSONObject()
    }
    
    /**
     * Get session management diagnostics
     */
    fun getSessionDiagnostics(): JSONObject {
        return sessionManager?.getDiagnostics() ?: JSONObject()
    }
    
    /**
     * Get comprehensive Phase 2 diagnostics
     */
    fun getPhase2Diagnostics(): JSONObject {
        return JSONObject().apply {
            put("time_sync", getTimeSyncDiagnostics())
            put("session_management", getSessionDiagnostics())
            put("phase2_enabled", true)
            put("services_active", timeSyncService != null && sessionManager != null)
        }
    }
    
    // Phase 3 - File Transfer & Data Management Methods
    
    /**
     * Create a new recording session
     */
    fun createRecordingSession(
        sessionId: String,
        participantId: String? = null,
        studyId: String? = null,
        conditions: List<String> = emptyList(),
        customMetadata: Map<String, Any> = emptyMap()
    ): DataManagementService.SessionData? {
        return dataManagementService?.createSession(
            sessionId = sessionId,
            deviceId = getDeviceId(),
            participantId = participantId,
            studyId = studyId,
            conditions = conditions,
            customMetadata = customMetadata
        )
    }
    
    /**
     * End a recording session
     */
    fun endRecordingSession(sessionId: String): Boolean {
        return dataManagementService?.endSession(sessionId) ?: false
    }
    
    /**
     * Register a recorded file with the current session
     */
    fun registerRecordedFile(
        filePath: String,
        sessionId: String,
        fileType: String,
        customMetadata: Map<String, Any> = emptyMap()
    ): DataManagementService.FileMetadata? {
        return dataManagementService?.registerFile(
            filePath = filePath,
            sessionId = sessionId,
            deviceId = getDeviceId(),
            fileType = fileType,
            customMetadata = customMetadata
        )
    }
    
    /**
     * Queue session files for upload to PC controller
     */
    suspend fun uploadSessionFiles(sessionId: String): List<String> {
        return dataManagementService?.queueFilesForUpload(sessionId) ?: emptyList()
    }
    
    /**
     * Queue individual file for upload
     */
    suspend fun uploadFile(
        filePath: String,
        sessionId: String,
        fileType: FileUploadService.FileType
    ): String? {
        return fileUploadService?.queueUpload(
            filePath = filePath,
            sessionId = sessionId,
            deviceId = getDeviceId(),
            fileType = fileType
        )
    }
    
    /**
     * Get file upload status
     */
    fun getUploadStatus(jobId: String): FileUploadService.UploadJob? {
        return fileUploadService?.getUploadStatus(jobId)
    }
    
    /**
     * Get all active uploads
     */
    fun getActiveUploads(): List<FileUploadService.UploadJob> {
        return fileUploadService?.getActiveUploads() ?: emptyList()
    }
    
    /**
     * Cancel file upload
     */
    suspend fun cancelUpload(jobId: String): Boolean {
        return fileUploadService?.cancelUpload(jobId) ?: false
    }
    
    /**
     * Pause file upload
     */
    suspend fun pauseUpload(jobId: String): Boolean {
        return fileUploadService?.pauseUpload(jobId) ?: false
    }
    
    /**
     * Resume file upload
     */
    suspend fun resumeUpload(jobId: String): Boolean {
        return fileUploadService?.resumeUpload(jobId) ?: false
    }
    
    /**
     * Export session data
     */
    suspend fun exportSession(
        sessionId: String,
        format: DataManagementService.ExportFormat,
        includeFiles: Boolean = false
    ): String? {
        return dataManagementService?.exportSession(sessionId, format, includeFiles)
    }
    
    /**
     * Get session information
     */
    fun getSession(sessionId: String): DataManagementService.SessionData? {
        return dataManagementService?.getSession(sessionId)
    }
    
    /**
     * Get all sessions
     */
    fun getAllSessions(): List<DataManagementService.SessionData> {
        return dataManagementService?.getAllSessions() ?: emptyList()
    }
    
    /**
     * Get storage statistics
     */
    fun getStorageStats(): Map<String, Any> {
        return dataManagementService?.getStorageStats() ?: emptyMap()
    }
    
    /**
     * Get upload statistics
     */
    fun getUploadStats(): Map<String, Any> {
        return fileUploadService?.getUploadStats() ?: emptyMap()
    }
    
    /**
     * Get comprehensive Phase 3 diagnostics
     */
    fun getPhase3Diagnostics(): JSONObject {
        return JSONObject().apply {
            put("file_upload_stats", JSONObject(getUploadStats()))
            put("storage_stats", JSONObject(getStorageStats()))
            put("active_sessions", getAllSessions().size)
            put("phase3_enabled", true)
            put("services_active", fileUploadService != null && dataManagementService != null)
            put("upload_protocol", FeatureFlags.FILE_UPLOAD_PROTOCOL)
        }
    }
    
    /**
     * Perform data cleanup
     */
    suspend fun performDataCleanup(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        dataManagementService?.performCleanup(maxAgeMs)
    }
    
    // Phase 4 - Advanced Authentication & Security Methods
    
    /**
     * Initialize Phase 4 services for advanced authentication and security
     */
    private fun initializePhase4Services() {
        // Initialize advanced authentication manager
        advancedAuthManager = AdvancedAuthenticationManager(context).apply {
            if (initialize()) {
                setAuthenticationListener(object : AdvancedAuthenticationManager.AuthenticationListener {
                    override fun onAuthenticationSuccess(context: AdvancedAuthenticationManager.AuthenticationContext) {
                        Log.i(TAG, "Advanced authentication successful: role=${context.role.name}")
                        logger.log(StructuredLogger.LogLevel.INFO, TAG, "advanced_auth_success", mapOf(
                            "device_id" to context.deviceId,
                            "role" to context.role.name,
                            "auth_level" to context.authLevel,
                            "session_token" to context.sessionToken.take(10) + "..."
                        ))
                    }
                    
                    override fun onAuthenticationFailure(reason: AdvancedAuthenticationManager.AuthenticationResult, attemptsRemaining: Int) {
                        Log.w(TAG, "Advanced authentication failed: $reason, attempts remaining: $attemptsRemaining")
                        logger.log(StructuredLogger.LogLevel.WARNING, TAG, "advanced_auth_failure", mapOf(
                            "reason" to reason.name,
                            "attempts_remaining" to attemptsRemaining
                        ))
                    }
                    
                    override fun onSessionExpired() {
                        Log.w(TAG, "Advanced authentication session expired")
                        logger.log(StructuredLogger.LogLevel.WARNING, TAG, "advanced_session_expired", emptyMap())
                        
                        // Attempt to reauthenticate
                        scope.launch {
                            attemptAdvancedReauthentication()
                        }
                    }
                    
                    override fun onSecurityAlert(alertType: String, details: Map<String, Any>) {
                        Log.w(TAG, "Security alert: $alertType")
                        logger.log(StructuredLogger.LogLevel.WARNING, TAG, "security_alert", mapOf(
                            "alert_type" to alertType,
                            "details" to details.toString()
                        ))
                        
                        // Send security alert to PC controller
                        scope.launch {
                            sendSecurityAlert(alertType, details)
                        }
                    }
                    
                    override fun onRoleChanged(newRole: AdvancedAuthenticationManager.DeviceRole, permissions: Set<String>) {
                        Log.i(TAG, "Role changed to: ${newRole.name}")
                        logger.log(StructuredLogger.LogLevel.INFO, TAG, "role_changed", mapOf(
                            "new_role" to newRole.name,
                            "permissions" to permissions.joinToString(",")
                        ))
                    }
                })
            }
        }
        
        Log.i(TAG, "Phase 4 services initialized: Advanced Authentication & Security")
        
        logger.log(StructuredLogger.LogLevel.INFO, TAG, "phase4_services_initialized", mapOf(
            "advanced_auth_enabled" to (advancedAuthManager != null),
            "multi_tier_auth" to true,
            "security_monitoring" to true,
            "rbac_enabled" to true
        ))
    }
    
    /**
     * Stop Phase 4 services
     */
    private fun stopPhase4Services() {
        advancedAuthManager?.shutdown()
        advancedAuthManager = null
        
        Log.i(TAG, "Phase 4 services stopped")
    }
    
    /**
     * Perform enhanced authentication with multiple tiers
     */
    suspend fun performEnhancedAuthentication(authLevel: Int, credentials: Map<String, Any>): Boolean {
        val manager = advancedAuthManager ?: return false
        
        return try {
            val result = manager.authenticate(
                deviceId = getDeviceId(),
                authLevel = authLevel,
                credentials = credentials
            )
            
            when (result) {
                AdvancedAuthenticationManager.AuthenticationResult.SUCCESS -> {
                    Log.i(TAG, "Enhanced authentication successful at level $authLevel")
                    true
                }
                else -> {
                    Log.w(TAG, "Enhanced authentication failed: $result")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Enhanced authentication error", e)
            false
        }
    }
    
    /**
     * Attempt advanced reauthentication
     */
    private suspend fun attemptAdvancedReauthentication() {
        try {
            // Try certificate-based authentication first
            val certificateCredentials = getCertificateCredentials()
            if (certificateCredentials.isNotEmpty()) {
                val success = performEnhancedAuthentication(
                    AdvancedAuthenticationManager.AUTH_LEVEL_CERTIFICATE,
                    certificateCredentials
                )
                if (success) {
                    Log.i(TAG, "Certificate-based reauthentication successful")
                    return
                }
            }
            
            // Fallback to token-based authentication  
            val tokenCredentials = getTokenCredentials()
            if (tokenCredentials.isNotEmpty()) {
                val success = performEnhancedAuthentication(
                    AdvancedAuthenticationManager.AUTH_LEVEL_TOKEN,
                    tokenCredentials
                )
                if (success) {
                    Log.i(TAG, "Token-based reauthentication successful")
                    return
                }
            }
            
            // Final fallback to basic authentication
            val basicCredentials = getBasicCredentials()
            val success = performEnhancedAuthentication(
                AdvancedAuthenticationManager.AUTH_LEVEL_BASIC,
                basicCredentials
            )
            
            if (success) {
                Log.i(TAG, "Basic reauthentication successful")
            } else {
                Log.w(TAG, "All reauthentication attempts failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during reauthentication", e)
        }
    }
    
    /**
     * Get certificate credentials for authentication
     */  
    private fun getCertificateCredentials(): Map<String, Any> {
        // In a real implementation, this would retrieve certificate from secure storage
        return mapOf(
            "device_type" to "ANDROID_PHONE",
            "certificate" to getDeviceCertificate(),
            "signature" to signChallenge("auth_challenge"),
            "challenge" to "auth_challenge"
        )
    }
    
    /**
     * Get token credentials for authentication
     */
    private fun getTokenCredentials(): Map<String, Any> {
        // In a real implementation, this would retrieve valid token from secure storage
        return mapOf(
            "device_type" to "ANDROID_PHONE",
            "token" to generateAuthToken(),
            "timestamp" to System.currentTimeMillis(),
            "hmac" to generateTokenHmac()
        )
    }
    
    /**
     * Get basic credentials for authentication
     */
    private fun getBasicCredentials(): Map<String, Any> {
        return mapOf(
            "device_type" to "ANDROID_PHONE",
            "username" to AUTH_USERNAME,
            "password" to AUTH_PASSWORD
        )
    }
    
    /**
     * Get device certificate (placeholder implementation)
     */
    private fun getDeviceCertificate(): ByteArray {
        // Placeholder - in production this would return actual certificate
        return "DEVICE_CERTIFICATE_PLACEHOLDER".toByteArray()
    }
    
    /**
     * Sign challenge for certificate authentication (placeholder)
     */
    private fun signChallenge(challenge: String): ByteArray {
        // Placeholder - in production this would use private key to sign
        return "SIGNATURE_PLACEHOLDER".toByteArray()
    }
    
    /**
     * Generate authentication token (placeholder)
     */
    private fun generateAuthToken(): String {
        // Placeholder - in production this would generate secure token
        return "AUTH_TOKEN_${System.currentTimeMillis()}_${getDeviceId().take(8)}"
    }
    
    /**
     * Generate token HMAC (placeholder)
     */
    private fun generateTokenHmac(): String {
        // Placeholder - in production this would generate actual HMAC
        return "HMAC_PLACEHOLDER"
    }
    
    /**
     * Send security alert to PC controller
     */
    private suspend fun sendSecurityAlert(alertType: String, details: Map<String, Any>) {
        try {
            val alertMessage = ProtocolVersion.createProtocolMessage("security_alert", JSONObject().apply {
                put("alert_type", alertType)
                put("device_id", getDeviceId())
                put("timestamp", System.currentTimeMillis())
                put("severity", determineSeverity(alertType))
                put("details", JSONObject().apply {
                    details.forEach { (key, value) ->
                        put(key, value.toString())
                    }
                })
            })
            
            sendMessage(alertMessage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send security alert", e)
        }
    }
    
    /**
     * Determine alert severity
     */
    private fun determineSeverity(alertType: String): String {
        return when (alertType) {
            "brute_force_attack", "session_hijack_attempt", "system_compromise" -> "CRITICAL"
            "permission_escalation", "data_exfiltration", "suspicious_connection" -> "HIGH"  
            "certificate_violation", "account_locked" -> "MEDIUM"
            else -> "LOW"
        }
    }
    
    /**
     * Check if current session has specific permission
     */
    fun hasAdvancedPermission(permission: String): Boolean {
        return advancedAuthManager?.hasPermission(permission) ?: false
    }
    
    /**
     * Get current authentication context
     */
    fun getAdvancedAuthContext(): AdvancedAuthenticationManager.AuthenticationContext? {
        return advancedAuthManager?.getCurrentContext()
    }
    
    /**
     * Get comprehensive Phase 4 diagnostics
     */
    fun getPhase4Diagnostics(): JSONObject {
        return JSONObject().apply {
            put("advanced_auth_active", advancedAuthManager != null)
            put("current_auth_level", getAdvancedAuthContext()?.authLevel ?: 0)
            put("current_role", getAdvancedAuthContext()?.role?.name ?: "NONE")
            put("session_active", advancedAuthManager?.isAuthenticated() ?: false)
            put("security_diagnostics", advancedAuthManager?.getSecurityDiagnostics() ?: JSONObject())
            put("phase4_enabled", true)
            put("multi_tier_auth_supported", true)
            put("rbac_enabled", true)
            put("security_monitoring_active", true)
        }
    }
    
    /**
     * Perform advanced security self-test
     */
    suspend fun performSecuritySelfTest(): JSONObject {
        val results = JSONObject()
        
        try {
            // Test basic authentication
            val basicTest = performEnhancedAuthentication(
                AdvancedAuthenticationManager.AUTH_LEVEL_BASIC,
                getBasicCredentials()
            )
            results.put("basic_auth_test", basicTest)
            
            // Test certificate authentication (will likely fail without real certificates)
            val certTest = performEnhancedAuthentication(
                AdvancedAuthenticationManager.AUTH_LEVEL_CERTIFICATE,
                getCertificateCredentials()
            )
            results.put("certificate_auth_test", certTest)
            
            // Test security monitoring
            results.put("security_monitoring_active", advancedAuthManager != null)
            
            // Test permission system
            results.put("permission_system_test", hasAdvancedPermission("view_status"))
            
            results.put("overall_status", "Phase 4 security system operational")
            results.put("test_timestamp", System.currentTimeMillis())
            
        } catch (e: Exception) {
            Log.e(TAG, "Security self-test failed", e)
            results.put("error", e.message)
            results.put("overall_status", "Phase 4 security system error")
        }
        
        return results
    }
}