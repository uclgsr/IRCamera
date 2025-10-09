package mpdc4gsr.feature.network.data

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class NetworkController(private val context: Context) {
    companion object {
        const val DEFAULT_PORT = 8080
        private const val SOCKET_TIMEOUT = 30000
        private const val BUFFER_SIZE = 4096
    }

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val clientConnections = ConcurrentHashMap<String, ClientConnection>()
    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var eventListener: NetworkControllerListener? = null

    interface NetworkControllerListener {
        fun onStartRecordingCommand(
            sessionId: String,
            modalities: List<String>,
            options: Map<String, Any>
        )

        fun onStopRecordingCommand()
        fun onClientConnected(clientId: String, clientInfo: String)
        fun onClientDisconnected(clientId: String, reason: String)
        fun onError(operation: String, error: String)
    }

    data class ClientConnection(
        val socket: Socket,
        val clientId: String,
        val inputStream: BufferedReader,
        val outputStream: PrintWriter,
        val connectedAt: Long = System.currentTimeMillis()
    )

    data class RemoteCommand(
        val command: String,
        val sessionId: String? = null,
        val modalities: List<String> = emptyList(),
        val options: Map<String, Any> = emptyMap()
    )

    fun setEventListener(listener: NetworkControllerListener) {
        this.eventListener = listener
    }

    suspend fun start(port: Int = DEFAULT_PORT): Boolean = withContext(Dispatchers.IO) {
        if (isRunning.get()) {
            return@withContext false
        }
        try {
            // First check if the port is available
            val actualPort = if (NetworkUtils.isPortAvailable(port)) {
                port
            } else {
                try {
                    val availablePort = NetworkUtils.findAvailablePort(port) availablePort
                } catch (e: IllegalStateException) {
                    eventListener?.onError(
                        "start_server",
                        "Port $port is already in use and no alternative ports available. Please ensure no other services are using ports ${port} to ${port + 9}."
                    )
                    return@withContext false
                }
            }
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(actualPort))
                soTimeout = SOCKET_TIMEOUT
            }
            isRunning.set(true) controllerScope . launch {
                acceptConnections()
            }
            return@withContext true
        } catch (e: java.net.BindException) {
            eventListener?.onError(
                "start_server",
                "Port $port is already in use. Please ensure no other services are using this port."
            )
            return@withContext false
        } catch (e: Exception) {
            eventListener?.onError("start_server", e.message ?: "Unknown error")
            return@withContext false
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        if (!isRunning.get()) {
            return@withContext
        }
        isRunning.set(false)
        try {
            // Close all client connections first
            val connectionsToClose = clientConnections.values.toList()
            connectionsToClose.forEach { connection ->
                try {
                    connection.outputStream.close()
                    connection.inputStream.close()
                    connection.socket.close()
                } catch (e: Exception) {
                    mpdc4gsr.core.utils.AppLogger.e("NetworkController", "Unexpected Exception in NetworkController catch block", e)
                }
            }
            clientConnections.clear()
            // Close server socket
            serverSocket?.let { socket ->
                try {
                    if (!socket.isClosed) {
                        socket.close()
                    }
                } catch (e: Exception) {
                    mpdc4gsr.core.utils.AppLogger.e("NetworkController", "Unexpected Exception in NetworkController catch block", e)
                }
            }
            serverSocket = null
            // Cancel coroutine scope
            controllerScope.cancel()
            // Small delay to allow cleanup to complete
            delay(100)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("NetworkController", "Unexpected Exception in NetworkController catch block", e)
        }
    }

    private suspend fun acceptConnections() = withContext(Dispatchers.IO) {
        while (isRunning.get() && serverSocket != null) {
            try {
                val clientSocket = serverSocket?.accept()
                if (clientSocket != null) {
                    handleNewClient(clientSocket)
                }
            } catch (e: SocketTimeoutException) {
                continue
            } catch (e: Exception) {
                if (isRunning.get()) {
                    eventListener?.onError("accept_connection", e.message ?: "Unknown error")
                }
                break
            }
        }
    }

    private suspend fun handleNewClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
            val inputStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val outputStream = PrintWriter(clientSocket.getOutputStream(), true)
            val connection = ClientConnection(
                socket = clientSocket,
                clientId = clientId,
                inputStream = inputStream,
                outputStream = outputStream
            )
            clientConnections[clientId] = connection
            eventListener?.onClientConnected(
                clientId,
                "PC Controller"
            ) sendResponse (connection, createResponse("welcome", "Connected to IRCamera Android"))
            controllerScope.launch {
                handleClientMessages(connection)
            }
        } catch (e: Exception) {
            try {
                clientSocket.close()
            } catch (ignored: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("NetworkController", "Unexpected Exception in NetworkController catch block", ignored)
            }
        }
    }

    private suspend fun handleClientMessages(connection: ClientConnection) =
        withContext(Dispatchers.IO) {
            try {
                while (isRunning.get() && !connection.socket.isClosed) {
                    val message = connection.inputStream.readLine()
                    if (message == null) {
                        // Client disconnected gracefully                        break
                    } handleCommand (connection, message)
                }
            } catch (e: SocketException) {
                // Handle connection reset and other socket exceptions gracefully
                when {
                    e.message?.contains("Connection reset") == true -> {
                        mpdc4gsr.core.utils.AppLogger.i(
                            "NetworkController",
                            "Client connection reset by peer (${connection.clientId})",
                            e,
                        )
                    }

                    e.message?.contains("Socket closed") == true -> {
                        mpdc4gsr.core.utils.AppLogger.d(
                            "NetworkController",
                            "Socket closed for client ${connection.clientId}",
                            e,
                        )
                    }

                    else -> {
                        mpdc4gsr.core.utils.AppLogger.w(
                            "NetworkController",
                            "Socket error for client ${connection.clientId}",
                            e,
                        )
                    }
                }
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e(
                    "NetworkController",
                    "Unhandled error while reading client messages for ${connection.clientId}",
                    e,
                )
            } finally {
                // Always disconnect the client to clean up resources
                disconnectClient(connection.clientId, "Connection closed")
            }
        }

    private suspend fun handleCommand(connection: ClientConnection, message: String) {
        try {
            val json = JSONObject(message)
            val command = json.getString("command")            when (command) {
                "start_recording" -> handleStartRecordingCommand(connection, json)
                "stop_recording" -> handleStopRecordingCommand(connection, json)
                "ping" -> handlePingCommand(connection, json)
                "get_status" -> handleGetStatusCommand(connection, json)
                else -> {
                    sendResponse(
                        connection,
                        createErrorResponse("unknown_command", "Unknown command: $command")
                    )
                }
            }
        } catch (e: Exception) {
            sendResponse(
                connection,
                createErrorResponse("parse_error", "Failed to parse command: ${e.message}")
            )
        }
    }

    private suspend fun handleStartRecordingCommand(
        connection: ClientConnection,
        json: JSONObject
    ) {
        try {
            val sessionId = json.optString("session_id", "session_${System.currentTimeMillis()}")
            val modalitiesArray = json.optJSONArray("modalities")
            if (modalitiesArray == null) {
                sendResponse(
                    connection,
                    createErrorResponse("invalid_request", "Missing or invalid 'modalities' field")
                )
                return
            }
            val modalities = mutableListOf<String>()
            for (i in 0 until modalitiesArray.length()) {
                modalities.add(modalitiesArray.getString(i))
            }
            val options = mutableMapOf<String, Any>()
            json.optBoolean("saveImages", false).let { options["saveImages"] = it }
            json.optInt("samplingRate", 64).let { options["samplingRate"] = it }
            json.optString("participantId", "").let {
                if (it.isNotEmpty()) options["participantId"] = it
            }
            json.optString("studyName", "").let {
                if (it.isNotEmpty()) options["studyName"] = it
            } eventListener ?. onStartRecordingCommand (sessionId, modalities, options)
            sendResponse(
                connection, createResponse(
                    "recording_started", "Recording session started", mapOf(
                        "session_id" to sessionId,
                        "modalities" to modalities
                    )
                )
            )
        } catch (e: Exception) {
            sendResponse(
                connection,
                createErrorResponse("start_recording_error", e.message ?: "Unknown error")
            )
        }
    }

    private suspend fun handleStopRecordingCommand(connection: ClientConnection, json: JSONObject) {
        try {
            eventListener?.onStopRecordingCommand()
            sendResponse(
                connection,
                createResponse("recording_stopped", "Recording session stopped")
            )
        } catch (e: Exception) {
            sendResponse(
                connection,
                createErrorResponse("stop_recording_error", e.message ?: "Unknown error")
            )
        }
    }

    private suspend fun handlePingCommand(connection: ClientConnection, json: JSONObject) {
        sendResponse(connection, createResponse("pong", "Server is alive"))
    }

    private suspend fun handleGetStatusCommand(connection: ClientConnection, json: JSONObject) {
        val status = mapOf(
            "connected_clients" to clientConnections.size,
            "server_running" to isRunning.get(),
            "server_uptime" to (System.currentTimeMillis() - connection.connectedAt)
        )
        sendResponse(connection, createResponse("status", "Server status", status))
    }

    private fun sendResponse(connection: ClientConnection, response: String) {
        try {
            connection.outputStream.println(response)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("NetworkController", "Unexpected Exception in NetworkController catch block", e)
        }
    }

    private fun createResponse(
        status: String,
        message: String,
        data: Map<String, Any> = emptyMap()
    ): String {
        val json = JSONObject()
        json.put("status", status)
        json.put("message", message)
        json.put("timestamp", System.currentTimeMillis())
        if (data.isNotEmpty()) {
            json.put("data", JSONObject(data))
        }
        return json.toString()
    }

    private fun createErrorResponse(error: String, message: String): String {
        val json = JSONObject()
        json.put("status", "error")
        json.put("error", error)
        json.put("message", message)
        json.put("timestamp", System.currentTimeMillis())
        return json.toString()
    }

    private fun disconnectClient(clientId: String, reason: String) {
        clientConnections[clientId]?.let { connection ->
            try {
                connection.socket.close()
            } catch (ignored: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("NetworkController", "Unexpected Exception in NetworkController catch block", ignored)
            }
            clientConnections.remove(clientId)
            eventListener?.onClientDisconnected(clientId, reason)
        }
    }

    fun getConnectedClientsCount(): Int = clientConnections.size
    fun isRunning(): Boolean = isRunning.get()

    fun getServerPort(): Int? = serverSocket?.localPort
}
