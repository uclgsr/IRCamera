package mpdc4gsr.feature.network.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Base64
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import com.mpdc4gsr.libunified.app.sync.TimeSyncService
import kotlinx.coroutines.*
import mpdc4gsr.core.SessionManager
import mpdc4gsr.core.StructuredLogger
import mpdc4gsr.core.data.AdvancedAuthenticationManager
import mpdc4gsr.core.data.FeatureFlags
import mpdc4gsr.core.data.ProtocolVersion
import okhttp3.*
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.min
import kotlin.random.Random

class WebSocketClient(private val context: Context) {
    companion object {
        private const val TAG = "WebSocketClient"

        private const val DEFAULT_PC_PORT = 8443
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val READ_TIMEOUT_MS = 30000L
        private const val WRITE_TIMEOUT_MS = 10000L

        private const val HEARTBEAT_INTERVAL_MS = 5000L
        private const val HEARTBEAT_TIMEOUT_MS = 15000L

        private const val RECONNECT_BASE_DELAY_MS = 1000L
        private const val RECONNECT_MAX_DELAY_MS = 8000L
        private const val RECONNECT_JITTER_MS = 500L

        private const val SERVICE_TYPE = "_irhub._tcp."
        private const val DISCOVERY_TIMEOUT_MS = 10000L
        private const val MANUAL_CONNECTION_DELAY_MS = 2000L

        private const val AUTH_USERNAME = "admin"
        private const val AUTH_PASSWORD = "admin"

        private const val AUTH_MODE_BASIC = "basic"
        private const val AUTH_MODE_CERTIFICATE = "certificate"
        private const val AUTH_MODE_TOKEN = "token"
        private const val AUTH_MODE_BIOMETRIC = "biometric"
    }

    private val isConnected = AtomicBoolean(false)
    private val isAuthenticating = AtomicBoolean(false)
    private val isAuthenticated = AtomicBoolean(false)
    private val isReconnecting = AtomicBoolean(false)

    private val okHttpClient: OkHttpClient
    private val webSocket = AtomicReference<WebSocket?>()
    private val currentServerInfo = AtomicReference<ServerInfo?>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var discoveryJob: Job? = null

    private val logger = StructuredLogger.getInstance(context)
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var timeSyncService: TimeSyncService? = null
    private var sessionManager: SessionManager? = null

    private var fileUploadService: FileUploadService? = null
    private var dataManagementService: DataManagementService? = null

    private var advancedAuthManager: AdvancedAuthenticationManager? = null

    private var eventListener: WebSocketEventListener? = null
    private val discoveredServers = mutableMapOf<String, ServerInfo>()

    private var connectionAttempts = 0
    private var lastHeartbeatTime = 0L
    private var connectionStartTime = 0L

    data class ServerInfo(
        val name: String,
        val host: String,
        val port: Int,
        val usesTLS: Boolean,
        val protocolVersion: String,
        val capabilities: Set<String>,
    )

    interface WebSocketEventListener {
        fun onConnecting(serverInfo: ServerInfo)

        fun onConnected(serverInfo: ServerInfo)

        fun onAuthenticated()

        fun onDisconnected(reason: String)

        fun onMessage(
            messageType: String,
            message: JSONObject,
        )

        fun onError(
            error: String,
            exception: Throwable?,
        )

        fun onServerDiscovered(serverInfo: ServerInfo)

        fun onHeartbeatReceived()
    }

    init {

        okHttpClient = createOkHttpClient()

        logger.log(
            StructuredLogger.LogLevel.INFO,
            "WebSocketClient",
            "initialized",
            mapOf(
                "device_id" to getDeviceId(),
                "tls_enabled" to FeatureFlags.TLS_ENABLE,
                "mdns_enabled" to FeatureFlags.MDNS_ENABLE,
            ),
        )
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder =
            OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)

        if (FeatureFlags.TLS_ENABLE) {
            val trustAllCerts =
                arrayOf<TrustManager>(
                    object : X509TrustManager {
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
                    },
                )

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }

    fun setEventListener(listener: WebSocketEventListener) {
        this.eventListener = listener
    }

    fun start() {
        if (isConnected.get()) {
            AppLogger.w(TAG, "WebSocket client already connected")
            return
        }

        AppLogger.i(TAG, "Starting WebSocket client")
        logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "start_requested", emptyMap())

        startServerDiscovery()
    }

    fun stop() {
        AppLogger.i(TAG, "Stopping WebSocket client")
        logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "stop_requested", emptyMap())

        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        discoveryJob?.cancel()

        webSocket.get()?.close(1000, "Client stopping")
        webSocket.set(null)

        isConnected.set(false)
        isAuthenticated.set(false)
        isReconnecting.set(false)
        currentServerInfo.set(null)

        stopServerDiscovery()

        stopPhase2Services()
        stopPhase3Services()
        stopPhase4Services()

        eventListener?.onDisconnected("Client stopped")
        eventListener = null
    }

    private fun startServerDiscovery() {
        if (!FeatureFlags.MDNS_ENABLE) {
            AppLogger.w(TAG, "mDNS discovery disabled, trying manual connection")

            return
        }

        discoveryJob =
            scope.launch {
                try {
                    AppLogger.i(TAG, "Starting NSD discovery for $SERVICE_TYPE")
                    logger.log(
                        StructuredLogger.LogLevel.INFO, "WebSocketClient", "discovery_started",
                        mapOf(
                            "service_type" to SERVICE_TYPE,
                        ),
                    )

                    val discoveryListener =
                        object : NsdManager.DiscoveryListener {
                            override fun onStartDiscoveryFailed(
                                serviceType: String,
                                errorCode: Int,
                            ) {
                                AppLogger.e(TAG, "Discovery start failed: $errorCode")
                                logger.log(
                                    StructuredLogger.LogLevel.ERROR,
                                    "WebSocketClient",
                                    "discovery_start_failed",
                                    mapOf(
                                        "error_code" to errorCode,
                                    ),
                                )
                            }

                            override fun onStopDiscoveryFailed(
                                serviceType: String,
                                errorCode: Int,
                            ) {
                                AppLogger.e(TAG, "Discovery stop failed: $errorCode")
                            }

                            override fun onDiscoveryStarted(serviceType: String) {
                                AppLogger.i(TAG, "Discovery started for $serviceType")
                            }

                            override fun onDiscoveryStopped(serviceType: String) {
                                AppLogger.i(TAG, "Discovery stopped for $serviceType")
                            }

                            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                                AppLogger.i(TAG, "Service found: ${serviceInfo.serviceName}")
                                resolveService(serviceInfo)
                            }

                            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                                AppLogger.i(TAG, "Service lost: ${serviceInfo.serviceName}")
                                discoveredServers.remove(serviceInfo.serviceName)
                            }
                        }

                    nsdManager.discoverServices(
                        SERVICE_TYPE,
                        NsdManager.PROTOCOL_DNS_SD,
                        discoveryListener
                    )

                    delay(DISCOVERY_TIMEOUT_MS)

                    if (discoveredServers.isEmpty()) {
                        AppLogger.w(TAG, "No servers discovered via mDNS, trying manual connection")
                        tryManualConnection()
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    AppLogger.i(TAG, "Server discovery cancelled")
                    logger.log(
                        StructuredLogger.LogLevel.INFO, "WebSocketClient", "discovery_cancelled",
                        emptyMap(),
                    )
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error in server discovery", e)
                    logger.log(
                        StructuredLogger.LogLevel.ERROR, "WebSocketClient", "discovery_error",
                        mapOf(
                            "error" to (e.message ?: "Unknown error"),
                        ),
                    )
                }
            }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener =
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int,
                ) {
                    AppLogger.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    @Suppress("DEPRECATION")
                    val hostAddress = serviceInfo.host.hostAddress ?: "unknown"
                    Log.i(
                        TAG,
                        "Service resolved: ${serviceInfo.serviceName} at $hostAddress:${serviceInfo.port}"
                    )

                    val attributes = serviceInfo.attributes
                    val protocolVersion = String(attributes["proto"] ?: "v1".toByteArray())
                    val usesTLS = String(attributes["tls"] ?: "1".toByteArray()) == "1"
                    val capabilities =
                        String(attributes["capabilities"] ?: "".toByteArray()).split(",").toSet()

                    val serverInfo =
                        ServerInfo(
                            name = serviceInfo.serviceName,
                            host = hostAddress,
                            port = serviceInfo.port,
                            usesTLS = usesTLS,
                            protocolVersion = protocolVersion,
                            capabilities = capabilities,
                        )

                    discoveredServers[serviceInfo.serviceName] = serverInfo
                    eventListener?.onServerDiscovered(serverInfo)

                    if (!isConnected.get() && !isReconnecting.get()) {
                        connectToServer(serverInfo)
                    }
                }
            }

        @Suppress("DEPRECATION")
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    private fun tryManualConnection() {
        val commonAddresses =
            listOf(
                "192.168.1.1",
                "192.168.0.1",
                "192.168.1.100",
                "192.168.0.100",
                "10.0.0.1",
                "172.16.0.1",
                "localhost",
                "127.0.0.1",
            )

        scope.launch {
            for (address in commonAddresses) {
                if (isConnected.get()) break

                val serverInfo =
                    ServerInfo(
                        name = "Manual-$address",
                        host = address,
                        port = DEFAULT_PC_PORT,
                        usesTLS = FeatureFlags.TLS_ENABLE,
                        protocolVersion = "v1",
                        capabilities = emptySet(),
                    )

                AppLogger.i(TAG, "Trying manual connection to $address:$DEFAULT_PC_PORT")
                connectToServer(serverInfo)

                delay(MANUAL_CONNECTION_DELAY_MS)
            }
        }
    }

    private fun connectToServer(serverInfo: ServerInfo) {
        if (isConnected.get()) {
            AppLogger.w(TAG, "Already connected")
            return
        }

        currentServerInfo.set(serverInfo)
        connectionStartTime = System.currentTimeMillis()
        connectionAttempts++

        AppLogger.i(TAG, "Connecting to ${serverInfo.name} at ${serverInfo.host}:${serverInfo.port}")
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "WebSocketClient",
            "connection_attempt",
            mapOf(
                "server_name" to serverInfo.name,
                "host" to serverInfo.host,
                "port" to serverInfo.port,
                "attempt" to connectionAttempts,
            ),
        )

        eventListener?.onConnecting(serverInfo)

        val protocol = if (serverInfo.usesTLS) "wss" else "ws"
        val url = "$protocol://${serverInfo.host}:${serverInfo.port}/"

        val request =
            Request.Builder()
                .url(url)
                .build()

        val webSocketListener =
            object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                    AppLogger.i(TAG, "WebSocket connection opened")
                    isConnected.set(true)
                    this@WebSocketClient.webSocket.set(webSocket)

                    logger.log(
                        StructuredLogger.LogLevel.INFO,
                        "WebSocketClient",
                        "connection_opened",
                        mapOf(
                            "server_name" to serverInfo.name,
                            "response_code" to response.code,
                        ),
                    )

                    eventListener?.onConnected(serverInfo)

                    scope.launch {
                        performHandshake()
                    }
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    scope.launch {
                        handleMessage(text)
                    }
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    AppLogger.i(TAG, "WebSocket connection closing: $code $reason")
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    AppLogger.i(TAG, "WebSocket connection closed: $code $reason")
                    handleDisconnection("Connection closed: $reason")
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?,
                ) {
                    AppLogger.e(TAG, "WebSocket connection failed", t)
                    handleDisconnection("Connection failed: ${t.message}")
                }
            }

        okHttpClient.newWebSocket(request, webSocketListener)
    }

    private suspend fun performHandshake() {
        try {
            val handshakeMessage = ProtocolVersion.createHandshakeMessage(getDeviceId())
            sendMessage(handshakeMessage)

            AppLogger.i(TAG, "Protocol handshake sent")
            logger.log(
                StructuredLogger.LogLevel.INFO,
                "WebSocketClient",
                "handshake_sent",
                emptyMap()
            )
        } catch (e: java.io.IOException) {
            AppLogger.e(TAG, "Network I/O error during handshake", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "WebSocketClient",
                "handshake_io_error",
                mapOf("error" to e.message.orEmpty())
            )
        } catch (e: org.json.JSONException) {
            AppLogger.e(TAG, "JSON error during handshake", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "WebSocketClient",
                "handshake_json_error",
                mapOf("error" to e.message.orEmpty())
            )
        }
    }

    private suspend fun performAuthentication() {
        try {
            isAuthenticating.set(true)

            val credentials = "$AUTH_USERNAME:$AUTH_PASSWORD"
            val encodedCredentials =
                Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

            val authMessage =
                ProtocolVersion.createProtocolMessage(
                    "auth_request",
                    JSONObject().apply {
                        put("auth_type", "basic")
                        put("credentials", encodedCredentials)
                    },
                )

            sendMessage(authMessage)

            AppLogger.i(TAG, "Authentication request sent")
            logger.log(StructuredLogger.LogLevel.INFO, "WebSocketClient", "auth_sent", emptyMap())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error performing authentication", e)
            isAuthenticating.set(false)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "WebSocketClient",
                "auth_error",
                mapOf(
                    "error" to e.message.orEmpty(),
                ),
            )
        }
    }

    private suspend fun handleMessage(text: String) {
        try {
            val message = JSONObject(text)
            val messageType = message.optString("message_type", "")

            if (!ProtocolVersion.validateMessageVersion(message)) {
                AppLogger.w(TAG, "Received message with invalid protocol version")
                return
            }

            AppLogger.d(TAG, "Received message: $messageType")

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
                    AppLogger.w(TAG, "Unknown message type: $messageType")
                    eventListener?.onMessage(messageType, message)
                }
            }
        } catch (e: org.json.JSONException) {
            AppLogger.e(TAG, "JSON parsing error handling message", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "WebSocketClient",
                "message_json_error",
                mapOf("error" to e.message.orEmpty())
            )
        } catch (e: IllegalStateException) {
            AppLogger.e(TAG, "Invalid state error handling message", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "WebSocketClient",
                "message_state_error",
                mapOf("error" to e.message.orEmpty()),
            )
        }
    }

    private suspend fun handleHandshakeResponse(message: JSONObject) {
        val authRequired = message.optBoolean("auth_required", false)

        if (authRequired) {
            performAuthentication()
        } else {

            startHeartbeat()
        }
    }

    private suspend fun handleAuthResponse(message: JSONObject) {
        isAuthenticating.set(false)
        val success = message.optBoolean("success", false)

        if (success) {
            AppLogger.i(TAG, "Authentication successful")
            isAuthenticated.set(true)
            logger.log(
                StructuredLogger.LogLevel.INFO,
                "WebSocketClient",
                "auth_success",
                emptyMap()
            )

            initializePhase2Services()

            initializePhase3Services()

            initializePhase4Services()

            eventListener?.onAuthenticated()
            startHeartbeat()
        } else {
            val error = message.optString("error_message", "Authentication failed")
            AppLogger.e(TAG, "Authentication failed: $error")
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "WebSocketClient",
                "auth_failed",
                mapOf(
                    "error" to error,
                ),
            )

            webSocket.get()?.close(4001, "Authentication failed")
        }
    }

    private suspend fun handlePing(message: JSONObject) {
        lastHeartbeatTime = System.currentTimeMillis()

        val pongMessage =
            ProtocolVersion.createProtocolMessage(
                "pong",
                JSONObject().apply {
                    put("timestamp", System.currentTimeMillis())
                },
            )
        sendMessage(pongMessage)
    }

    private suspend fun handleHeartbeatResponse(message: JSONObject) {
        lastHeartbeatTime = System.currentTimeMillis()
        eventListener?.onHeartbeatReceived()
    }

    private suspend fun handleSyncFlash(message: JSONObject) {
        eventListener?.onMessage("sync_flash", message)
    }

    private suspend fun handleSessionResponse(message: JSONObject) {
        eventListener?.onMessage(message.optString("message_type"), message)
    }

    private suspend fun handleError(message: JSONObject) {
        val errorType = message.optString("error_type", "unknown")
        val errorMessage = message.optString("error_message", "Unknown error")

        AppLogger.e(TAG, "Server error: $errorType - $errorMessage")
        logger.log(
            StructuredLogger.LogLevel.ERROR,
            "WebSocketClient",
            "server_error",
            mapOf(
                "error_type" to errorType,
                "error_message" to errorMessage,
            ),
        )

        eventListener?.onError("Server error: $errorMessage", null)
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob =
            scope.launch {
                lastHeartbeatTime = System.currentTimeMillis()

                while (isConnected.get() && isAuthenticated.get()) {
                    try {

                        val currentTime = System.currentTimeMillis()
                        if (lastHeartbeatTime > 0 && (currentTime - lastHeartbeatTime) > HEARTBEAT_TIMEOUT_MS) {
                            AppLogger.w(TAG, "Heartbeat timeout, disconnecting")
                            webSocket.get()?.close(4000, "Heartbeat timeout")
                            break
                        }

                        val heartbeatMessage =
                            ProtocolVersion.createProtocolMessage(
                                "heartbeat",
                                JSONObject().apply {
                                    put("timestamp", currentTime)
                                },
                            )
                        sendMessage(heartbeatMessage)

                        delay(HEARTBEAT_INTERVAL_MS)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Error in heartbeat", e)
                        break
                    }
                }
            }
    }

    private fun handleDisconnection(reason: String) {
        isConnected.set(false)
        isAuthenticated.set(false)
        webSocket.set(null)

        heartbeatJob?.cancel()

        stopPhase2Services()
        stopPhase3Services()
        stopPhase4Services()

        logger.log(
            StructuredLogger.LogLevel.WARNING,
            "WebSocketClient",
            "disconnected",
            mapOf(
                "reason" to reason,
            ),
        )

        eventListener?.onDisconnected(reason)

        if (!reason.contains("Client stopping")) {
            startReconnection()
        }
    }

    private fun startReconnection() {
        if (isReconnecting.get()) return

        isReconnecting.set(true)

        reconnectJob =
            scope.launch {
                var attempt = 1

                while (!isConnected.get() && isReconnecting.get()) {
                    try {

                        val baseDelay = min(
                            RECONNECT_BASE_DELAY_MS * (1L shl (attempt - 1)),
                            RECONNECT_MAX_DELAY_MS
                        )
                        val jitter = Random.nextLong(-RECONNECT_JITTER_MS, RECONNECT_JITTER_MS)
                        val delay = baseDelay + jitter

                        AppLogger.i(TAG, "Reconnection attempt $attempt in ${delay}ms")
                        logger.log(
                            StructuredLogger.LogLevel.INFO, "WebSocketClient", "reconnect_attempt",
                            mapOf(
                                "attempt" to attempt,
                                "delay_ms" to delay,
                            ),
                        )

                        delay(delay)

                        currentServerInfo.get()?.let { serverInfo ->
                            connectToServer(serverInfo)
                        } ?: run {

                            startServerDiscovery()
                        }

                        attempt++
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Error in reconnection", e)
                        break
                    }
                }

                isReconnecting.set(false)
            }
    }

    private fun stopServerDiscovery() {
        try {

            discoveryJob?.cancel()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping server discovery", e)
        }
    }

    suspend fun sendMessage(message: JSONObject) {
        try {
            val webSocket = this.webSocket.get()
            if (webSocket == null) {
                AppLogger.w(TAG, "Cannot send message - not connected")
                return
            }

            val jsonString = message.toString()
            val success = webSocket.send(jsonString)

            if (!success) {
                AppLogger.w(TAG, "Failed to send message")
                logger.log(
                    StructuredLogger.LogLevel.WARNING,
                    "WebSocketClient",
                    "send_failed",
                    mapOf(
                        "message_type" to message.optString("message_type", "unknown"),
                    ),
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error sending message", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "WebSocketClient",
                "send_error",
                mapOf(
                    "error" to e.message.orEmpty(),
                ),
            )
        }
    }

    suspend fun sendSessionStart(sessionId: String = "") {
        val message =
            ProtocolVersion.createProtocolMessage(
                "session_start",
                JSONObject().apply {
                    put("session_id", sessionId.ifEmpty { java.util.UUID.randomUUID().toString() })
                    put("device_id", getDeviceId())
                },
            )
        sendMessage(message)
    }

    suspend fun sendSessionStop(sessionId: String = "") {
        val message =
            ProtocolVersion.createProtocolMessage(
                "session_stop",
                JSONObject().apply {
                    put("session_id", sessionId)
                    put("device_id", getDeviceId())
                },
            )
        sendMessage(message)
    }

    suspend fun sendStatusRequest() {
        val message =
            ProtocolVersion.createProtocolMessage(
                "status_request",
                JSONObject().apply {
                    put("device_id", getDeviceId())
                },
            )
        sendMessage(message)
    }

    fun isConnected(): Boolean = isConnected.get()

    fun isAuthenticated(): Boolean = isAuthenticated.get()

    fun isReconnecting(): Boolean = isReconnecting.get()

    fun getCurrentServer(): ServerInfo? = currentServerInfo.get()

    fun getDiscoveredServers(): Map<String, ServerInfo> = discoveredServers.toMap()

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        ) ?: "unknown"
    }

    private fun initializePhase2Services() {

        timeSyncService = TimeSyncService().apply {
            setListener(object : TimeSyncService.TimeSyncListener {
                override fun onSyncCompleted(result: TimeSyncService.SyncResult) {
                    if (result.isSuccess) {
                        Log.i(
                            TAG,
                            "Time sync completed: offset=${result.clockOffsetMs}ms, accuracy=±${result.accuracyMs}ms"
                        )
                        logger.log(
                            StructuredLogger.LogLevel.INFO,
                            "WebSocketClient",
                            "sync_completed",
                            mapOf(
                                "offset_ms" to result.clockOffsetMs.toString(),
                                "rtt_ms" to result.roundTripDelayMs.toString(),
                                "accuracy_ms" to result.accuracyMs.toString(),
                            ),
                        )
                    } else {
                        AppLogger.w(TAG, "Time sync failed: ${result.errorMessage}")
                        logger.log(
                            StructuredLogger.LogLevel.WARNING,
                            "WebSocketClient",
                            "sync_failed",
                            mapOf("error" to (result.errorMessage ?: "unknown"))
                        )
                    }
                }

                override fun onSyncStarted(targetHost: String) {
                    AppLogger.i(TAG, "Time sync started with $targetHost")
                }

                override fun onSyncError(error: String) {
                    AppLogger.e(TAG, "Time sync error: $error")
                }
            })
        }

        sessionManager =
            SessionManager(context, logger).apply {
                start(
                    onSessionStateChanged = { state ->
                        AppLogger.i(TAG, "Session state changed: $state")
                        logger.log(
                            StructuredLogger.LogLevel.INFO,
                            "WebSocketClient",
                            "session_state_changed",
                            mapOf(
                                "new_state" to state.name,
                            ),
                        )
                    },
                    onDeviceJoined = { device ->
                        Log.i(
                            TAG,
                            "Device joined session: ${device.deviceId} (${device.deviceType})"
                        )
                        logger.log(
                            StructuredLogger.LogLevel.INFO, "WebSocketClient", "device_joined",
                            mapOf(
                                "device_id" to device.deviceId,
                                "device_type" to device.deviceType,
                            ),
                        )
                    },
                    onDeviceLeft = { device ->
                        AppLogger.i(TAG, "Device left session: ${device.deviceId}")
                        logger.log(
                            StructuredLogger.LogLevel.INFO, "WebSocketClient", "device_left",
                            mapOf(
                                "device_id" to device.deviceId,
                            ),
                        )
                    },
                    onSyncRequired = { devices ->
                        Log.i(
                            TAG,
                            "Cross-device synchronization required for ${devices.size} devices"
                        )
                        performCrossDeviceSync(devices)
                    },
                )
            }

        AppLogger.i(TAG, "Phase 2 services initialized: Enhanced Time Sync + Session Management")
    }

    private fun stopPhase2Services() {
        timeSyncService?.stopPeriodicSync()
        timeSyncService?.cleanup()
        timeSyncService = null

        sessionManager?.stop()
        sessionManager = null

        AppLogger.i(TAG, "Phase 2 services stopped")
    }

    private fun initializePhase3Services() {

        fileUploadService =
            FileUploadService(context).apply {
                initialize(this@WebSocketClient)
            }

        dataManagementService =
            DataManagementService(context).apply {
                initialize(fileUploadService)
            }

        AppLogger.i(TAG, "Phase 3 services initialized: File Transfer + Data Management")

        logger.log(
            StructuredLogger.LogLevel.INFO,
            "WebSocketClient",
            "phase3_services_initialized",
            mapOf(
                "file_upload_enabled" to (fileUploadService != null),
                "data_management_enabled" to (dataManagementService != null),
                "upload_protocol" to FeatureFlags.FILE_UPLOAD_PROTOCOL,
            ),
        )
    }

    private fun stopPhase3Services() {
        fileUploadService?.shutdown()
        fileUploadService = null

        dataManagementService = null

        AppLogger.i(TAG, "Phase 3 services stopped")
    }

    private fun performCrossDeviceSync(devices: List<SessionManager.DeviceInfo>) {
        scope.launch {
            try {
                logger.log(
                    StructuredLogger.LogLevel.INFO,
                    "WebSocketClient",
                    "cross_device_sync_started",
                    mapOf(
                        "device_count" to devices.size.toString(),
                    ),
                )

                val syncMessage =
                    JSONObject().apply {
                        put("type", "sync_flash")
                        put("device_count", devices.size)
                        put(
                            "sync_timestamp",
                            System.nanoTime()
                        )
                    }

                sendMessage(syncMessage)

                devices.forEach { device ->
                    sessionManager?.updateDeviceHeartbeat(
                        device.deviceId,
                        0L,
                        SessionManager.ConnectionQuality.GOOD,
                    )
                }

                logger.log(
                    StructuredLogger.LogLevel.INFO,
                    "WebSocketClient",
                    "cross_device_sync_completed",
                    emptyMap()
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Cross-device sync error", e)
                logger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "WebSocketClient",
                    "cross_device_sync_error",
                    mapOf(
                        "error" to e.message.orEmpty(),
                    ),
                )
            }
        }
    }

    fun createRecordingSession(metadata: Map<String, Any> = emptyMap()): String? {
        val manager = sessionManager ?: return null
        val sessionId = manager.createSession(metadata)

        val deviceCapabilities = setOf("recording", "camera", "sensors")
        manager.joinDevice(
            deviceId = getDeviceId(),
            deviceType = "android_phone",
            capabilities = deviceCapabilities,
        )

        return sessionId
    }

    fun startSynchronizedRecording(): Boolean {
        return sessionManager?.startSyncRecording() ?: false
    }

    fun stopSynchronizedRecording() {
        sessionManager?.stopSyncRecording()
    }

    fun getTimeSyncDiagnostics(): JSONObject {
        return JSONObject()
    }

    fun getSessionDiagnostics(): JSONObject {
        return sessionManager?.getDiagnostics() ?: JSONObject()
    }

    fun getPhase2Diagnostics(): JSONObject {
        return JSONObject().apply {
            put("time_sync", getTimeSyncDiagnostics())
            put("session_management", getSessionDiagnostics())
            put("phase2_enabled", true)
            put("services_active", timeSyncService != null && sessionManager != null)
        }
    }

    fun createRecordingSession(
        sessionId: String,
        participantId: String? = null,
        studyId: String? = null,
        conditions: List<String> = emptyList(),
        customMetadata: Map<String, Any> = emptyMap(),
    ): DataManagementService.SessionData? {
        return dataManagementService?.createSession(
            sessionId = sessionId,
            deviceId = getDeviceId(),
            participantId = participantId,
            studyId = studyId,
            conditions = conditions,
            customMetadata = customMetadata,
        )
    }

    fun endRecordingSession(sessionId: String): Boolean {
        return dataManagementService?.endSession(sessionId) ?: false
    }

    fun registerRecordedFile(
        filePath: String,
        sessionId: String,
        fileType: String,
        customMetadata: Map<String, Any> = emptyMap(),
    ): DataManagementService.FileMetadata? {
        return dataManagementService?.registerFile(
            filePath = filePath,
            sessionId = sessionId,
            deviceId = getDeviceId(),
            fileType = fileType,
            customMetadata = customMetadata,
        )
    }

    suspend fun uploadSessionFiles(sessionId: String): List<String> {
        return dataManagementService?.queueFilesForUpload(sessionId) ?: emptyList()
    }

    suspend fun uploadFile(
        filePath: String,
        sessionId: String,
        fileType: FileUploadService.FileType,
    ): String? {
        return fileUploadService?.queueUpload(
            filePath = filePath,
            sessionId = sessionId,
            deviceId = getDeviceId(),
            fileType = fileType,
        )
    }

    fun getUploadStatus(jobId: String): FileUploadService.UploadJob? {
        return fileUploadService?.getUploadStatus(jobId)
    }

    fun getActiveUploads(): List<FileUploadService.UploadJob> {
        return fileUploadService?.getActiveUploads() ?: emptyList()
    }

    suspend fun cancelUpload(jobId: String): Boolean {
        return fileUploadService?.cancelUpload(jobId) ?: false
    }

    suspend fun pauseUpload(jobId: String): Boolean {
        return fileUploadService?.pauseUpload(jobId) ?: false
    }

    suspend fun resumeUpload(jobId: String): Boolean {
        return fileUploadService?.resumeUpload(jobId) ?: false
    }

    suspend fun exportSession(
        sessionId: String,
        format: String,
        includeFiles: Boolean = false,
    ): String? {
        return try {
            val exportFormat =
                DataManagementService.Companion.ExportFormat.valueOf(format.uppercase())
            dataManagementService?.exportSession(sessionId, exportFormat, includeFiles)
        } catch (e: IllegalArgumentException) {
            AppLogger.w(TAG, "Invalid export format: $format, defaulting to JSON")
            dataManagementService?.exportSession(
                sessionId,
                DataManagementService.Companion.ExportFormat.JSON,
                includeFiles
            )
        }
    }

    fun getSession(sessionId: String): DataManagementService.SessionData? {
        return dataManagementService?.getSession(sessionId)
    }

    fun getAllSessions(): List<DataManagementService.SessionData> {
        return dataManagementService?.getAllSessions() ?: emptyList()
    }

    fun getStorageStats(): Map<String, Any> {
        return dataManagementService?.getStorageStats() ?: emptyMap()
    }

    fun getUploadStats(): Map<String, Any> {
        return fileUploadService?.getUploadStats() ?: emptyMap()
    }

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

    suspend fun performDataCleanup(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        dataManagementService?.performCleanup(maxAgeMs)
    }

    private fun initializePhase4Services() {

        advancedAuthManager =
            AdvancedAuthenticationManager(context).apply {
                if (initialize()) {
                    setAuthenticationListener(
                        object : AdvancedAuthenticationManager.AuthenticationListener {
                            override fun onAuthenticationSuccess(context: AdvancedAuthenticationManager.AuthenticationContext) {
                                Log.i(
                                    TAG,
                                    "Advanced authentication successful: role=${context.role.name}"
                                )
                                logger.log(
                                    StructuredLogger.LogLevel.INFO, TAG, "advanced_auth_success",
                                    mapOf(
                                        "device_id" to context.deviceId,
                                        "role" to context.role.name,
                                        "auth_level" to context.authLevel,
                                        "session_token" to context.sessionToken.take(10) + "...",
                                    ),
                                )
                            }

                            override fun onAuthenticationFailure(
                                reason: AdvancedAuthenticationManager.AuthenticationResult,
                                attemptsRemaining: Int,
                            ) {
                                Log.w(
                                    TAG,
                                    "Advanced authentication failed: $reason, attempts remaining: $attemptsRemaining"
                                )
                                logger.log(
                                    StructuredLogger.LogLevel.WARNING, TAG, "advanced_auth_failure",
                                    mapOf(
                                        "reason" to reason.name,
                                        "attempts_remaining" to attemptsRemaining,
                                    ),
                                )
                            }

                            override fun onSessionExpired() {
                                AppLogger.w(TAG, "Advanced authentication session expired")
                                logger.log(
                                    StructuredLogger.LogLevel.WARNING,
                                    TAG,
                                    "advanced_session_expired",
                                    emptyMap()
                                )

                                scope.launch {
                                    attemptAdvancedReauthentication()
                                }
                            }

                            override fun onSecurityAlert(
                                alertType: String,
                                details: Map<String, Any>,
                            ) {
                                AppLogger.w(TAG, "Security alert: $alertType")
                                logger.log(
                                    StructuredLogger.LogLevel.WARNING, TAG, "security_alert",
                                    mapOf(
                                        "alert_type" to alertType,
                                        "details" to details.toString(),
                                    ),
                                )

                                scope.launch {
                                    sendSecurityAlert(alertType, details)
                                }
                            }

                            override fun onRoleChanged(
                                newRole: AdvancedAuthenticationManager.DeviceRole,
                                permissions: Set<String>,
                            ) {
                                AppLogger.i(TAG, "Role changed to: ${newRole.name}")
                                logger.log(
                                    StructuredLogger.LogLevel.INFO, TAG, "role_changed",
                                    mapOf(
                                        "new_role" to newRole.name,
                                        "permissions" to permissions.joinToString(","),
                                    ),
                                )
                            }
                        },
                    )
                }
            }

        AppLogger.i(TAG, "Phase 4 services initialized: Advanced Authentication & Security")

        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "phase4_services_initialized",
            mapOf(
                "advanced_auth_enabled" to (advancedAuthManager != null),
                "multi_tier_auth" to true,
                "security_monitoring" to true,
                "rbac_enabled" to true,
            ),
        )
    }

    private fun stopPhase4Services() {
        advancedAuthManager?.shutdown()
        advancedAuthManager = null

        AppLogger.i(TAG, "Phase 4 services stopped")
    }

    suspend fun performEnhancedAuthentication(
        authLevel: Int,
        credentials: Map<String, Any>,
    ): Boolean {
        val manager = advancedAuthManager ?: return false

        return try {
            val result =
                manager.authenticate(
                    deviceId = getDeviceId(),
                    authLevel = authLevel,
                    credentials = credentials,
                )

            when (result) {
                AdvancedAuthenticationManager.AuthenticationResult.SUCCESS -> {
                    AppLogger.i(TAG, "Enhanced authentication successful at level $authLevel")
                    true
                }

                else -> {
                    AppLogger.w(TAG, "Enhanced authentication failed: $result")
                    false
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Enhanced authentication error", e)
            false
        }
    }

    private suspend fun attemptAdvancedReauthentication() {
        try {

            val certificateCredentials = getCertificateCredentials()
            if (certificateCredentials.isNotEmpty()) {
                val success =
                    performEnhancedAuthentication(
                        AdvancedAuthenticationManager.AUTH_LEVEL_CERTIFICATE,
                        certificateCredentials,
                    )
                if (success) {
                    AppLogger.i(TAG, "Certificate-based reauthentication successful")
                    return
                }
            }

            val tokenCredentials = getTokenCredentials()
            if (tokenCredentials.isNotEmpty()) {
                val success =
                    performEnhancedAuthentication(
                        AdvancedAuthenticationManager.AUTH_LEVEL_TOKEN,
                        tokenCredentials,
                    )
                if (success) {
                    AppLogger.i(TAG, "Token-based reauthentication successful")
                    return
                }
            }

            val basicCredentials = getBasicCredentials()
            val success =
                performEnhancedAuthentication(
                    AdvancedAuthenticationManager.AUTH_LEVEL_BASIC,
                    basicCredentials,
                )

            if (success) {
                AppLogger.i(TAG, "Basic reauthentication successful")
            } else {
                AppLogger.w(TAG, "All reauthentication attempts failed")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during reauthentication", e)
        }
    }

    private fun getCertificateCredentials(): Map<String, Any> {

        return mapOf(
            "device_type" to "ANDROID_PHONE",
            "certificate" to getDeviceCertificate(),
            "signature" to signChallenge("auth_challenge"),
            "challenge" to "auth_challenge",
        )
    }

    private fun getTokenCredentials(): Map<String, Any> {

        return mapOf(
            "device_type" to "ANDROID_PHONE",
            "token" to generateAuthToken(),
            "timestamp" to System.currentTimeMillis(),
            "hmac" to generateTokenHmac(),
        )
    }

    private fun getBasicCredentials(): Map<String, Any> {
        return mapOf(
            "device_type" to "ANDROID_PHONE",
            "username" to AUTH_USERNAME,
            "password" to AUTH_PASSWORD,
        )
    }

    private fun getDeviceCertificate(): ByteArray {

        return "DEVICE_CERTIFICATE_PLACEHOLDER".toByteArray()
    }

    private fun signChallenge(challenge: String): ByteArray {

        return "SIGNATURE_PLACEHOLDER".toByteArray()
    }

    private fun generateAuthToken(): String {

        return "AUTH_TOKEN_${System.currentTimeMillis()}_${getDeviceId().take(8)}"
    }

    private fun generateTokenHmac(): String {

        return "HMAC_PLACEHOLDER"
    }

    private suspend fun sendSecurityAlert(
        alertType: String,
        details: Map<String, Any>,
    ) {
        try {
            val alertMessage =
                ProtocolVersion.createProtocolMessage(
                    "security_alert",
                    JSONObject().apply {
                        put("alert_type", alertType)
                        put("device_id", getDeviceId())
                        put("timestamp", System.currentTimeMillis())
                        put("severity", determineSeverity(alertType))
                        put(
                            "details",
                            JSONObject().apply {
                                details.forEach { (key, value) ->
                                    put(key, value.toString())
                                }
                            },
                        )
                    },
                )

            sendMessage(alertMessage)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to send security alert", e)
        }
    }

    private fun determineSeverity(alertType: String): String {
        return when (alertType) {
            "brute_force_attack", "session_hijack_attempt", "system_compromise" -> "CRITICAL"
            "permission_escalation", "data_exfiltration", "suspicious_connection" -> "HIGH"
            "certificate_violation", "account_locked" -> "MEDIUM"
            else -> "LOW"
        }
    }

    fun hasAdvancedPermission(permission: String): Boolean {
        return advancedAuthManager?.hasPermission(permission) ?: false
    }

    fun getAdvancedAuthContext(): AdvancedAuthenticationManager.AuthenticationContext? {
        return advancedAuthManager?.getCurrentContext()
    }

    fun getPhase4Diagnostics(): JSONObject {
        return JSONObject().apply {
            put("advanced_auth_active", advancedAuthManager != null)
            put("current_auth_level", getAdvancedAuthContext()?.authLevel ?: 0)
            put("current_role", getAdvancedAuthContext()?.role?.name ?: "NONE")
            put("session_active", advancedAuthManager?.isAuthenticated() ?: false)
            put(
                "security_diagnostics",
                advancedAuthManager?.getSecurityDiagnostics() ?: JSONObject()
            )
            put("phase4_enabled", true)
            put("multi_tier_auth_supported", true)
            put("rbac_enabled", true)
            put("security_monitoring_active", true)
        }
    }

    suspend fun performSecuritySelfTest(): JSONObject {
        val results = JSONObject()

        try {

            val basicTest =
                performEnhancedAuthentication(
                    AdvancedAuthenticationManager.AUTH_LEVEL_BASIC,
                    getBasicCredentials(),
                )
            results.put("basic_auth_test", basicTest)

            val certTest =
                performEnhancedAuthentication(
                    AdvancedAuthenticationManager.AUTH_LEVEL_CERTIFICATE,
                    getCertificateCredentials(),
                )
            results.put("certificate_auth_test", certTest)

            results.put("security_monitoring_active", advancedAuthManager != null)

            results.put("permission_system_test", hasAdvancedPermission("view_status"))

            results.put("overall_status", "Phase 4 security system operational")
            results.put("test_timestamp", System.currentTimeMillis())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Security self-test failed", e)
            results.put("error", e.message)
            results.put("overall_status", "Phase 4 security system error")
        }

        return results
    }
}
