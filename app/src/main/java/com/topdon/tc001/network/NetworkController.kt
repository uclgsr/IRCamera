package com.topdon.tc001.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

/**
 * NetworkController implements the JSON command protocol for PC remote orchestration.
 * Handles start_recording/stop_recording commands as specified in the requirements.
 */
class NetworkController(private val context: Context) {

    companion object {
        private const val TAG = "NetworkController"
        const val DEFAULT_PORT = 8080
        private const val SOCKET_TIMEOUT = 30000 // 30 seconds
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

    /**
     * Start the network controller server to listen for PC connections
     */
    suspend fun start(port: Int = DEFAULT_PORT): Boolean = withContext(Dispatchers.IO) {
        if (isRunning.get()) {
            Log.w(TAG, "NetworkController already running")
            return@withContext false
        }

        try {
            serverSocket = ServerSocket(port)
            serverSocket?.soTimeout = SOCKET_TIMEOUT
            isRunning.set(true)

            Log.i(TAG, "NetworkController started on port $port")

            // Start accepting client connections
            controllerScope.launch {
                acceptConnections()
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start NetworkController", e)
            eventListener?.onError("start_server", e.message ?: "Unknown error")
            return@withContext false
        }
    }

    /**
     * Stop the network controller server
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        isRunning.set(false)

        try {
            // Close all client connections
            clientConnections.values.forEach { connection ->
                try {
                    connection.socket.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing client connection: ${e.message}")
                }
            }
            clientConnections.clear()

            // Close server socket
            serverSocket?.close()
            serverSocket = null

            // Cancel all coroutines and wait for completion
            controllerScope.coroutineContext.cancelChildren()
            runBlocking {
                controllerScope.coroutineContext.job.join()
            }

            Log.i(TAG, "NetworkController stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping NetworkController", e)
        }
    }

    /**
     * Accept incoming client connections
     */
    private suspend fun acceptConnections() = withContext(Dispatchers.IO) {
        while (isRunning.get() && serverSocket != null) {
            try {
                val clientSocket = serverSocket?.accept()
                if (clientSocket != null) {
                    handleNewClient(clientSocket)
                }
            } catch (e: SocketTimeoutException) {
                // Timeout is expected, continue loop
                continue
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Error accepting client connection", e)
                    eventListener?.onError("accept_connection", e.message ?: "Unknown error")
                }
                break
            }
        }
    }

    /**
     * Handle new client connection
     */
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
            eventListener?.onClientConnected(clientId, "PC Controller")

            Log.i(TAG, "New client connected: $clientId")

            // Send welcome message
            sendResponse(connection, createResponse("welcome", "Connected to IRCamera Android"))

            // Start handling messages from this client
            controllerScope.launch {
                handleClientMessages(connection)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling new client", e)
            try {
                clientSocket.close()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Handle messages from a connected client
     */
    private suspend fun handleClientMessages(connection: ClientConnection) =
        withContext(Dispatchers.IO) {
            try {
                while (isRunning.get() && !connection.socket.isClosed) {
                    val message = connection.inputStream.readLine()
                    if (message == null) {
                        // Client disconnected
                        break
                    }

                    Log.d(TAG, "Received message from ${connection.clientId}: $message")
                    handleCommand(connection, message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client messages", e)
            } finally {
                // Clean up connection
                disconnectClient(connection.clientId, "Connection closed")
            }
        }

    /**
     * Handle JSON command from PC controller
     */
    private suspend fun handleCommand(connection: ClientConnection, message: String) {
        try {
            val json = JSONObject(message)
            val command = json.getString("command")

            Log.i(TAG, "Processing command: $command")

            when (command) {
                "start_recording" -> handleStartRecordingCommand(connection, json)
                "stop_recording" -> handleStopRecordingCommand(connection, json)
                "ping" -> handlePingCommand(connection, json)
                "get_status" -> handleGetStatusCommand(connection, json)
                else -> {
                    Log.w(TAG, "Unknown command: $command")
                    sendResponse(
                        connection,
                        createErrorResponse("unknown_command", "Unknown command: $command")
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing command", e)
            sendResponse(
                connection,
                createErrorResponse("parse_error", "Failed to parse command: ${e.message}")
            )
        }
    }

    /**
     * Handle start_recording command
     */
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

            // Extract options
            val options = mutableMapOf<String, Any>()
            json.optBoolean("saveImages", false).let { options["saveImages"] = it }
            json.optInt("samplingRate", 64).let { options["samplingRate"] = it }
            json.optString("participantId", "").let {
                if (it.isNotEmpty()) options["participantId"] = it
            }
            json.optString("studyName", "").let {
                if (it.isNotEmpty()) options["studyName"] = it
            }

            Log.i(TAG, "Start recording command: sessionId=$sessionId, modalities=$modalities")

            // Notify listener
            eventListener?.onStartRecordingCommand(sessionId, modalities, options)

            // Send acknowledgment
            sendResponse(
                connection, createResponse(
                    "recording_started", "Recording session started", mapOf(
                        "session_id" to sessionId,
                        "modalities" to modalities
                    )
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error handling start_recording command", e)
            sendResponse(
                connection,
                createErrorResponse("start_recording_error", e.message ?: "Unknown error")
            )
        }
    }

    /**
     * Handle stop_recording command
     */
    private suspend fun handleStopRecordingCommand(connection: ClientConnection, json: JSONObject) {
        try {
            Log.i(TAG, "Stop recording command received")

            // Notify listener
            eventListener?.onStopRecordingCommand()

            // Send acknowledgment
            sendResponse(
                connection,
                createResponse("recording_stopped", "Recording session stopped")
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error handling stop_recording command", e)
            sendResponse(
                connection,
                createErrorResponse("stop_recording_error", e.message ?: "Unknown error")
            )
        }
    }

    /**
     * Handle ping command (for connection testing)
     */
    private suspend fun handlePingCommand(connection: ClientConnection, json: JSONObject) {
        sendResponse(connection, createResponse("pong", "Server is alive"))
    }

    /**
     * Handle get_status command
     */
    private suspend fun handleGetStatusCommand(connection: ClientConnection, json: JSONObject) {
        val status = mapOf(
            "connected_clients" to clientConnections.size,
            "server_running" to isRunning.get(),
            "server_uptime" to (System.currentTimeMillis() - connection.connectedAt)
        )

        sendResponse(connection, createResponse("status", "Server status", status))
    }

    /**
     * Send response to client
     */
    private fun sendResponse(connection: ClientConnection, response: String) {
        try {
            connection.outputStream.println(response)
            Log.d(TAG, "Sent response to ${connection.clientId}: $response")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending response to ${connection.clientId}", e)
        }
    }

    /**
     * Create success response JSON
     */
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

    /**
     * Create error response JSON
     */
    private fun createErrorResponse(error: String, message: String): String {
        val json = JSONObject()
        json.put("status", "error")
        json.put("error", error)
        json.put("message", message)
        json.put("timestamp", System.currentTimeMillis())

        return json.toString()
    }

    /**
     * Disconnect a client
     */
    private fun disconnectClient(clientId: String, reason: String) {
        clientConnections[clientId]?.let { connection ->
            try {
                connection.socket.close()
            } catch (ignored: Exception) {
            }

            clientConnections.remove(clientId)
            eventListener?.onClientDisconnected(clientId, reason)

            Log.i(TAG, "Client disconnected: $clientId - $reason")
        }
    }

    /**
     * Get number of connected clients
     */
    fun getConnectedClientsCount(): Int = clientConnections.size

    /**
     * Check if server is running
     */
    fun isRunning(): Boolean = isRunning.get()
}
