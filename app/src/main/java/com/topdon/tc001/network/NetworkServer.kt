package com.topdon.tc001.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TCP Server for accepting connections from PC Controller.
 *
 * This server implements the protocol expected by the PC Controller test scripts,
 * allowing the PC to remotely control the Android device via JSON commands over TCP.
 *
 * Protocol:
 * - PC connects to Android on port 8080
 * - Messages are sent as: 4-byte length (big-endian) + JSON payload
 * - Android processes commands and sends responses in same format
 *
 * @author IRCamera Android Sensor Node (Spoke)
 */
class NetworkServer(
    private val context: Context,
    private val port: Int = 8080,
) {
    companion object {
        private const val TAG = "NetworkServer"
        private const val MAX_MESSAGE_SIZE = 10 * 1024 * 1024 // 10MB limit
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null

    private val isRunning = AtomicBoolean(false)
    private val isClientConnected = AtomicBoolean(false)
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Flow for incoming messages
    private val _messageFlow = MutableSharedFlow<JSONObject>()
    val messageFlow: SharedFlow<JSONObject> = _messageFlow.asSharedFlow()

    // Flow for connection state
    private val _connectionStateFlow = MutableStateFlow(false)
    val connectionStateFlow: StateFlow<Boolean> = _connectionStateFlow.asStateFlow()

    private var serverJob: Job? = null
    private var messageListenerJob: Job? = null

    /**
     * Start the TCP server
     */
    suspend fun start(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isRunning.get()) {
                    Log.w(TAG, "Server already running")
                    return@withContext true
                }

                Log.i(TAG, "Starting TCP server on port $port")

                serverSocket = ServerSocket(port)
                isRunning.set(true)

                // Start server job to accept connections
                serverJob =
                    serverScope.launch {
                        acceptConnections()
                    }

                Log.i(TAG, "TCP server started successfully on port $port")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start TCP server", e)
                isRunning.set(false)
                return@withContext false
            }
        }
    }

    /**
     * Stop the TCP server
     */
    suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Stopping TCP server")

                isRunning.set(false)
                isClientConnected.set(false)
                _connectionStateFlow.value = false

                // Cancel jobs
                serverJob?.cancel()
                messageListenerJob?.cancel()

                // Close client connection
                outputStream?.close()
                inputStream?.close()
                clientSocket?.close()

                // Close server socket
                serverSocket?.close()

                outputStream = null
                inputStream = null
                clientSocket = null
                serverSocket = null

                Log.i(TAG, "TCP server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping TCP server", e)
            }
        }
    }

    /**
     * Send a message to the connected PC
     */
    suspend fun sendMessage(message: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get() || outputStream == null) {
                    Log.w(TAG, "No client connected, cannot send message")
                    return@withContext false
                }

                val messageData = message.toString().toByteArray(Charsets.UTF_8)

                // Send length first (4 bytes, big-endian) then message
                outputStream!!.writeInt(messageData.size)
                outputStream!!.write(messageData)
                outputStream!!.flush()

                Log.d(TAG, "Sent message to PC: $message")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to PC", e)
                // Connection might be broken, disconnect client
                disconnectClient()
                return@withContext false
            }
        }
    }

    /**
     * Accept incoming connections from PC Controller
     */
    private suspend fun acceptConnections() {
        while (isRunning.get() && !serverJob?.isCancelled!!) {
            try {
                Log.i(TAG, "Waiting for PC Controller connection...")

                val socket = serverSocket?.accept()
                if (socket != null && isRunning.get()) {
                    Log.i(TAG, "PC Controller connected from ${socket.remoteSocketAddress}")

                    // Disconnect any existing client first
                    disconnectClient()

                    // Setup new client connection
                    clientSocket = socket
                    outputStream = DataOutputStream(socket.getOutputStream())
                    inputStream = DataInputStream(socket.getInputStream())

                    isClientConnected.set(true)
                    _connectionStateFlow.value = true

                    // Start message listener for this client
                    messageListenerJob =
                        serverScope.launch {
                            listenForMessages()
                        }
                }
            } catch (e: SocketException) {
                if (isRunning.get()) {
                    Log.e(TAG, "Socket error accepting connections", e)
                } else {
                    Log.i(TAG, "Server socket closed normally")
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting connection", e)
                delay(1000) // Wait before trying again
            }
        }
    }

    /**
     * Listen for messages from connected PC
     */
    private suspend fun listenForMessages() {
        while (isClientConnected.get() && isRunning.get() && !messageListenerJob?.isCancelled!!) {
            try {
                val message = receiveMessage()
                if (message != null) {
                    _messageFlow.emit(message)
                } else {
                    // Connection lost
                    break
                }
            } catch (e: SocketException) {
                Log.i(TAG, "PC Controller disconnected")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error receiving message from PC", e)
                break
            }
        }

        // Clean up client connection
        disconnectClient()
    }

    /**
     * Receive a message from PC using the test script protocol
     */
    private suspend fun receiveMessage(): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val input = inputStream ?: return@withContext null

                // Read message length (4 bytes, big-endian)
                val messageLength = input.readInt()

                if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
                    Log.e(TAG, "Invalid message length: $messageLength")
                    return@withContext null
                }

                // Read the message data
                val messageData = ByteArray(messageLength)
                input.readFully(messageData)

                val messageJson = String(messageData, Charsets.UTF_8)
                val message = JSONObject(messageJson)

                Log.d(TAG, "Received message from PC: $messageJson")
                return@withContext message
            } catch (e: Exception) {
                Log.e(TAG, "Error receiving message", e)
                return@withContext null
            }
        }
    }

    /**
     * Disconnect the current client
     */
    private fun disconnectClient() {
        if (isClientConnected.get()) {
            Log.i(TAG, "Disconnecting PC Controller client")

            isClientConnected.set(false)
            _connectionStateFlow.value = false

            messageListenerJob?.cancel()

            try {
                outputStream?.close()
                inputStream?.close()
                clientSocket?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing client connection", e)
            }

            outputStream = null
            inputStream = null
            clientSocket = null
        }
    }

    /**
     * Check if server is running
     */
    fun isRunning(): Boolean = isRunning.get()

    /**
     * Check if PC is connected
     */
    fun isClientConnected(): Boolean = isClientConnected.get()

    /**
     * Clean up server resources
     */
    suspend fun cleanup() {
        stop()
        serverScope.cancel()
        Log.i(TAG, "Network server cleaned up")
    }
}
