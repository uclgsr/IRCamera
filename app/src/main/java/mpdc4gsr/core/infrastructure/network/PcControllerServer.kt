package mpdc4gsr.core.infrastructure.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.crash.CrashSafeSupervisor
import mpdc4gsr.core.common.logging.StructuredLogger
import mpdc4gsr.core.data.ProtocolVersion
import mpdc4gsr.feature.connectivity.data.NetworkUtils
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Extracts the custom PC controller socket server out of [mpdc4gsr.app.runtime.RecordingService].
 * The class owns the lifecycle of the server socket, NSD registration, client handling and
 * message processing so that the service can focus on high-level session orchestration.
 */
class PcControllerServer(
    private val context: Context,
    private val serviceScope: CoroutineScope,
    private val structuredLogger: StructuredLogger,
    private val crashSafeSupervisor: CrashSafeSupervisor,
    private val deviceId: String,
    private val callbacks: Callbacks,
    private val listener: Listener,
    private val config: Config = Config(),
) {
    data class Config(
        val basePort: Int = DEFAULT_PORT,
        val serviceName: String = DEFAULT_SERVICE_NAME,
        val serviceType: String = DEFAULT_SERVICE_TYPE,
        val enableMdns: Boolean = true,
    )

    interface Callbacks {
        suspend fun onSessionStartRequested(sessionDirectory: File)

        suspend fun onSessionStopRequested()

        suspend fun onSyncFlashRequested(durationMs: Int)

        suspend fun provideRecordingState(): RecordingState
    }

    data class RecordingState(
        val isRecording: Boolean,
        val currentSessionDirectory: String?,
        val sensorsInitialized: Boolean,
    )

    interface Listener {
        fun onServerStarted(port: Int)

        fun onServerStopped()

        fun onClientCountChanged(count: Int)

        fun onServerFailure(throwable: Throwable)
    }

    data class Status(
        val isRunning: Boolean,
        val port: Int,
        val connectedClients: List<String>,
    )

    companion object {
        private const val TAG = "PcControllerServer"
        private const val JOB_ID = "server_socket"
        private const val DEFAULT_SERVICE_NAME = "IRCamera-Android"
        private const val DEFAULT_SERVICE_TYPE = "_ircamera._tcp."
        private const val DEFAULT_PORT = 8081
        private const val SOCKET_TIMEOUT_MS = 30_000
    }

    private data class ClientConnection(
        val socket: Socket,
        val clientId: String,
        val input: DataInputStream,
        val output: DataOutputStream,
        val job: Job,
    )

    private val activeConnections = ConcurrentHashMap<String, ClientConnection>()
    private val isRunning = AtomicBoolean(false)
    private val actualPort = AtomicInteger(config.basePort)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var nsdListener: NsdManager.RegistrationListener? = null
    private val nsdRegistered = AtomicBoolean(false)

    private val healthCheck =
        object : CrashSafeSupervisor.HealthCheck {
            override suspend fun checkHealth(): CrashSafeSupervisor.HealthStatus {
                val running = isRunning.get() && serverSocket?.isClosed == false
                return if (running) {
                    CrashSafeSupervisor.HealthStatus(
                        isHealthy = true,
                        message = "Server socket listening",
                        details =
                            mapOf(
                                "port" to actualPort.get(),
                                "client_count" to activeConnections.size,
                                "mdns_registered" to nsdRegistered.get(),
                            ),
                    )
                } else {
                    CrashSafeSupervisor.HealthStatus(
                        isHealthy = false,
                        message = "Server socket stopped or closed",
                    )
                }
            }
        }

    fun start() {
        if (serverJob?.isActive == true || isRunning.get()) {
            structuredLogger.logServerEvent(
                "server_already_running",
                mapOf("port" to actualPort.get()),
            )
            return
        }
        serverJob =
            crashSafeSupervisor.registerJob(
                id = JOB_ID,
                name = "PC Controller Server",
                critical = true,
                restartable = true,
                healthCheck = healthCheck,
            ) { stopToken ->
                runServer(stopToken)
            }
    }

    fun stop() {
        crashSafeSupervisor.unregisterJob(JOB_ID)
        cleanupServer()
    }

    fun shutdown() {
        stop()
    }

    fun isRunning(): Boolean = isRunning.get()

    fun currentPort(): Int = actualPort.get()

    fun describeStatus(): String =
        if (isRunning()) {
            "Running on port ${currentPort()} (${activeConnections.size} clients)"
        } else {
            "Stopped"
        }

    fun connectedClients(): List<String> = activeConnections.keys().asSequence().toList()

    fun status(): Status = Status(isRunning(), currentPort(), connectedClients())

    private suspend fun runServer(stopToken: CrashSafeSupervisor.StopToken) {
        try {
            delayBeforeStart()
            val boundPort = bindServerSocket()
            isRunning.set(true)
            structuredLogger.logServerEvent(
                "server_socket_started",
                mapOf(
                    "port" to boundPort,
                    "preferred_port" to config.basePort,
                ),
            )
            notifyServerStarted(boundPort)
            if (config.enableMdns) {
                registerNsdService(boundPort)
            }
            acceptLoop(stopToken)
        } catch (bindException: BindException) {
            structuredLogger.logServerEvent(
                "server_socket_bind_failed",
                mapOf(
                    "port" to config.basePort,
                    "error" to (bindException.message ?: "port_in_use"),
                ),
            )
            listener.onServerFailure(bindException)
            throw bindException
        } catch (expected: Exception) {
            structuredLogger.logServerEvent(
                "server_socket_failed",
                mapOf("error" to (expected.message ?: "unknown_error")),
            )
            listener.onServerFailure(expected)
            throw expected
        } finally {
            cleanupServer()
        }
    }

    private suspend fun acceptLoop(stopToken: CrashSafeSupervisor.StopToken) {
        while (!stopToken.isStopRequested() && isRunning.get()) {
            try {
                val clientSocket = withContext(Dispatchers.IO) { serverSocket?.accept() }
                if (clientSocket != null && isRunning.get()) {
                    handleNewClient(clientSocket)
                }
            } catch (socketException: SocketException) {
                if (isRunning.get() && !stopToken.isStopRequested()) {
                    structuredLogger.logServerEvent(
                        "accept_socket_error",
                        mapOf("error" to (socketException.message ?: "unknown_error")),
                    )
                    delayOnError()
                }
            } catch (generic: Exception) {
                if (isRunning.get() && !stopToken.isStopRequested()) {
                    structuredLogger.logServerEvent(
                        "accept_unexpected_error",
                        mapOf("error" to (generic.message ?: "unknown_error")),
                    )
                    delayOnError(longDelay = true)
                }
            }
        }
    }

    private fun handleNewClient(socket: Socket) {
        try {
            socket.soTimeout = SOCKET_TIMEOUT_MS
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            val clientId = "${socket.inetAddress.hostAddress}:${socket.port}"
            structuredLogger.logConnection(
                "pc_client_connected",
                clientId,
                mapOf("client_address" to (socket.inetAddress.hostAddress ?: "unknown")),
            )
            val readerJob =
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        handleClientMessages(clientId, input, output)
                    } catch (cancellationException: CancellationException) {
                        structuredLogger.logConnection(
                            "client_handler_cancelled",
                            clientId,
                            mapOf("reason" to (cancellationException.message ?: "cancelled")),
                        )
                    } catch (e: Exception) {
                        structuredLogger.logConnection(
                            "client_handler_error",
                            clientId,
                            mapOf("error" to (e.message ?: "unknown_error")),
                        )
                    } finally {
                        removeClient(clientId)
                        closeQuietly(socket)
                        notifyClientCountChanged()
                    }
                }
            activeConnections[clientId] =
                ClientConnection(socket, clientId, input, output, readerJob)
            notifyClientCountChanged()
        } catch (e: Exception) {
            structuredLogger.logServerEvent(
                "client_accept_failure",
                mapOf("error" to (e.message ?: "unknown_error")),
            )
            closeQuietly(socket)
        }
    }

    private suspend fun handleClientMessages(
        clientId: String,
        input: DataInputStream,
        output: DataOutputStream,
    ) {
        while (isRunning.get() && currentCoroutineContext().isActive) {
            try {
                val messageLength = input.readInt()
                if (messageLength > MAX_MESSAGE_BYTES) {
                    structuredLogger.logProtocolMessage(
                        "message_too_large",
                        messageId = "overflow_$clientId",
                        connectionId = clientId,
                        details = mapOf("size_bytes" to messageLength),
                    )
                    break
                }
                val messageData = ByteArray(messageLength)
                input.readFully(messageData)
                val message = JSONObject(String(messageData, Charsets.UTF_8))
                processClientMessage(clientId, message, output)
            } catch (timeout: SocketTimeoutException) {
                sendKeepAlive(output)
            } catch (_: EOFException) {
                break
            } catch (socketClosed: SocketException) {
                structuredLogger.logConnection(
                    "socket_closed",
                    clientId,
                    mapOf("reason" to (socketClosed.message ?: "socket_exception")),
                )
                break
            } catch (unknown: Exception) {
                structuredLogger.logConnection(
                    "message_processing_error",
                    clientId,
                    mapOf("error" to (unknown.message ?: "unknown_error")),
                )
                break
            }
        }
    }

    private suspend fun processClientMessage(
        clientId: String,
        message: JSONObject,
        output: DataOutputStream,
    ) {
        val messageType = message.optString("message_type")
        val messageId = message.optString("msg_id", "unknown")
        structuredLogger.logProtocolMessage(
            "message_received",
            messageId,
            clientId,
            mapOf(
                "message_type" to messageType,
                "protocol_version" to message.optString("protocol_version", "unknown"),
            ),
        )
        if (!ProtocolVersion.validateMessageVersion(message)) {
            val errorMsg = "Unsupported protocol version"
            structuredLogger.logProtocolMessage(
                "protocol_version_error",
                messageId,
                clientId,
                mapOf("error" to errorMsg),
            )
            sendError(output, errorMsg)
            return
        }
        when (messageType) {
            "protocol_handshake" -> handleHandshake(clientId, messageId, message, output)
            "session_start" -> handleSessionStart(clientId, message, output)
            "session_stop" -> handleSessionStop(clientId, output)
            "sync_flash" -> handleSyncFlash(clientId, message, output)
            "status_request" -> sendStatusResponse(output)
            "heartbeat" -> handleHeartbeat(clientId, messageId, message, output)
            else -> handleUnknownMessage(clientId, messageType, output)
        }
    }

    private suspend fun handleHandshake(
        clientId: String,
        messageId: String,
        payload: JSONObject,
        output: DataOutputStream,
    ) {
        val handshakeResult = ProtocolVersion.validateHandshakeResponse(payload)
        if (handshakeResult.success) {
            structuredLogger.logProtocolMessage(
                "handshake_success",
                messageId,
                clientId,
                mapOf(
                    "negotiated_version" to (handshakeResult.negotiatedVersion ?: "unknown"),
                    "capabilities" to handshakeResult.commonCapabilities.joinToString(","),
                ),
            )
            val response = ProtocolVersion.createHandshakeMessage(deviceId)
            sendMessage(output, response)
        } else {
            structuredLogger.logProtocolMessage(
                "handshake_failed",
                messageId,
                clientId,
                mapOf("error" to (handshakeResult.error ?: "unknown")),
            )
            sendError(output, handshakeResult.error ?: "Handshake failed")
        }
    }

    private suspend fun handleSessionStart(
        clientId: String,
        payload: JSONObject,
        output: DataOutputStream,
    ) {
        val sessionId = payload.optString("session_id", "remote_${System.currentTimeMillis()}")
        val sessionName = payload.optString("session_name", "PC Remote Session")
        structuredLogger.logSessionEvent(
            "remote_session_start_request",
            sessionId,
            mapOf("session_name" to sessionName, "client_id" to clientId),
        )
        val baseDir =
            File(context.getExternalFilesDir(null), "recordings").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        val sessionDir = File(baseDir, sessionId)
        callbacks.onSessionStartRequested(sessionDir)
        val ackPayload =
            ProtocolVersion.createProtocolMessage(
                "ack",
                JSONObject().apply {
                    put("ack_for", "session_start")
                    put("result", "Recording started")
                    put("session_id", sessionId)
                },
            )
        sendMessage(output, ackPayload)
    }

    private suspend fun handleSessionStop(
        clientId: String,
        output: DataOutputStream,
    ) {
        structuredLogger.logSessionEvent(
            "remote_session_stop_request",
            "current",
            mapOf("client_id" to clientId),
        )
        callbacks.onSessionStopRequested()
        val ackPayload =
            ProtocolVersion.createProtocolMessage(
                "ack",
                JSONObject().apply {
                    put("ack_for", "session_stop")
                    put("result", "Recording stopped")
                },
            )
        sendMessage(output, ackPayload)
    }

    private suspend fun handleSyncFlash(
        clientId: String,
        payload: JSONObject,
        output: DataOutputStream,
    ) {
        val durationMs = payload.optInt("duration_ms", 100)
        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "SyncFlash",
            "remote_sync_flash_request",
            mapOf("duration_ms" to durationMs, "client_id" to clientId),
        )
        callbacks.onSyncFlashRequested(durationMs)
        val ackPayload =
            ProtocolVersion.createProtocolMessage(
                "ack",
                JSONObject().apply {
                    put("ack_for", "sync_flash")
                    put("result", "Flash performed")
                },
            )
        sendMessage(output, ackPayload)
    }

    private suspend fun handleHeartbeat(
        clientId: String,
        messageId: String,
        payload: JSONObject,
        output: DataOutputStream,
    ) {
        structuredLogger.logProtocolMessage(
            "heartbeat_received",
            messageId,
            clientId,
            mapOf("timestamp" to payload.optLong("timestamp", 0)),
        )
        val ackPayload =
            ProtocolVersion.createProtocolMessage(
                "ack",
                JSONObject().apply {
                    put("ack_for", "heartbeat")
                    put("result", "alive")
                },
            )
        sendMessage(output, ackPayload)
    }

    private suspend fun handleUnknownMessage(
        clientId: String,
        messageType: String,
        output: DataOutputStream,
    ) {
        structuredLogger.logProtocolMessage(
            "unknown_message_type",
            messageType,
            clientId,
            mapOf("message_type" to messageType),
        )
        val errorPayload =
            ProtocolVersion.createProtocolMessage(
                "error",
                JSONObject().apply {
                    put("error", "Unknown message type: $messageType")
                },
            )
        sendMessage(output, errorPayload)
    }

    private suspend fun sendStatusResponse(output: DataOutputStream) {
        val state = callbacks.provideRecordingState()
        val statusMessage =
            JSONObject().apply {
                put("message_type", "status_response")
                put("device_id", deviceId)
                put("recording_active", state.isRecording)
                put("connected_clients", activeConnections.size)
                put("server_running", isRunning.get())
                put("sensors_initialized", state.sensorsInitialized)
                put("current_session", state.currentSessionDirectory ?: "")
                put("timestamp", System.currentTimeMillis())
            }
        sendMessage(output, statusMessage)
    }

    private suspend fun sendError(
        output: DataOutputStream,
        error: String,
    ) {
        val errorMessage =
            JSONObject().apply {
                put("message_type", "error")
                put("error", error)
                put("timestamp", System.currentTimeMillis())
            }
        sendMessage(output, errorMessage)
    }

    private suspend fun sendMessage(
        output: DataOutputStream,
        message: JSONObject,
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (!message.has("protocol_version")) {
                    message.put("protocol_version", ProtocolVersion.CURRENT_VERSION)
                }
                val payload = message.toString().toByteArray(Charsets.UTF_8)
                output.writeInt(payload.size)
                output.write(payload)
                output.flush()
                structuredLogger.log(
                    StructuredLogger.LogLevel.DEBUG,
                    "ServerSocket",
                    "message_sent",
                    mapOf(
                        "message_type" to message.optString("message_type", "unknown"),
                        "size_bytes" to payload.size,
                    ),
                )
            } catch (e: Exception) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "ServerSocket",
                    "message_send_error",
                    mapOf("error" to (e.message ?: "unknown_error")),
                )
                throw e
            }
        }
    }

    private suspend fun sendKeepAlive(output: DataOutputStream) {
        val keepAliveMessage =
            JSONObject().apply {
                put("message_type", "keepalive")
                put("timestamp", System.currentTimeMillis())
            }
        sendMessage(output, keepAliveMessage)
    }

    private fun cleanupServer() {
        val wasRunning = isRunning.getAndSet(false)
        if (!wasRunning) {
            return
        }
        serverJob = null
        structuredLogger.logServerEvent("server_socket_cleanup_started")
        activeConnections.values.forEach { connection ->
            try {
                connection.job.cancel()
                closeQuietly(connection.socket)
            } catch (e: Exception) {
                structuredLogger.logConnection(
                    "connection_cleanup_error",
                    connection.clientId,
                    mapOf("error" to (e.message ?: "unknown_error")),
                )
            }
        }
        activeConnections.clear()
        closeQuietly(serverSocket)
        serverSocket = null
        unregisterNsdService()
        structuredLogger.logServerEvent("server_socket_cleanup_completed")
        notifyClientCountChanged()
        notifyServerStopped()
    }

    private fun notifyServerStarted(port: Int) {
        serviceScope.launch(Dispatchers.Main) {
            listener.onServerStarted(port)
        }
    }

    private fun notifyServerStopped() {
        serviceScope.launch(Dispatchers.Main) {
            listener.onServerStopped()
        }
    }

    private fun notifyClientCountChanged() {
        serviceScope.launch(Dispatchers.Main) {
            listener.onClientCountChanged(activeConnections.size)
        }
    }

    private suspend fun delayBeforeStart() {
        // Give the OS a brief moment to release any lingering sockets.
        delay(100)
    }

    private suspend fun delayOnError(longDelay: Boolean = false) {
        delay(if (longDelay) 5_000 else 1_000)
    }

    private fun bindServerSocket(): Int {
        val resolvedPort =
            if (NetworkUtils.isPortAvailable(config.basePort)) {
                config.basePort
            } else {
                NetworkUtils.findAvailablePort(config.basePort + 1, maxAttempts = 10)
            }
        val socket =
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(resolvedPort))
            }
        serverSocket = socket
        actualPort.set(resolvedPort)
        return resolvedPort
    }

    private fun registerNsdService(port: Int) {
        if (nsdManager == null || nsdRegistered.get()) {
            return
        }
        try {
            val serviceInfo =
                NsdServiceInfo().apply {
                    serviceName = config.serviceName
                    serviceType = config.serviceType
                    this.port = port
                }
            nsdListener =
                object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                        nsdRegistered.set(true)
                        structuredLogger.logServerEvent(
                            "nsd_registered",
                            mapOf("service_name" to serviceInfo?.serviceName.orEmpty()),
                        )
                    }

                    override fun onRegistrationFailed(
                        serviceInfo: NsdServiceInfo?,
                        errorCode: Int,
                    ) {
                        nsdRegistered.set(false)
                        structuredLogger.logServerEvent(
                            "nsd_registration_failed",
                            mapOf("error_code" to errorCode),
                        )
                    }

                    override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                        nsdRegistered.set(false)
                        structuredLogger.logServerEvent(
                            "nsd_service_unregistered",
                            mapOf("service_name" to serviceInfo?.serviceName.orEmpty()),
                        )
                    }

                    override fun onUnregistrationFailed(
                        serviceInfo: NsdServiceInfo?,
                        errorCode: Int,
                    ) {
                        structuredLogger.logServerEvent(
                            "nsd_unregistration_failed",
                            mapOf("error_code" to errorCode),
                        )
                    }
                }
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                nsdListener,
            )
        } catch (e: Exception) {
            nsdRegistered.set(false)
            structuredLogger.logServerEvent(
                "nsd_registration_exception",
                mapOf("error" to (e.message ?: "unknown_error")),
            )
        }
    }

    private fun unregisterNsdService() {
        val listener = nsdListener ?: return
        if (nsdManager == null || !nsdRegistered.get()) {
            return
        }
        try {
            nsdManager.unregisterService(listener)
        } catch (e: Exception) {
            structuredLogger.logServerEvent(
                "nsd_unregistration_exception",
                mapOf("error" to (e.message ?: "unknown_error")),
            )
        } finally {
            nsdListener = null
            nsdRegistered.set(false)
        }
    }

    private fun removeClient(clientId: String) {
        activeConnections.remove(clientId)
        structuredLogger.logConnection("pc_client_disconnected", clientId)
    }

    private fun closeQuietly(socket: ServerSocket?) {
        try {
            socket?.close()
        } catch (ignored: Exception) {
            mpdc4gsr.core.common.AppLogger
                .e("PcControllerServer", "Unexpected Exception in PcControllerServer catch block", ignored)
        }
    }

    private fun closeQuietly(socket: Socket?) {
        try {
            socket?.close()
        } catch (ignored: Exception) {
            mpdc4gsr.core.common.AppLogger
                .e("PcControllerServer", "Unexpected Exception in PcControllerServer catch block", ignored)
        }
    }

    private companion object {
        private const val MAX_MESSAGE_BYTES = 1 * 1024 * 1024
    }
}

