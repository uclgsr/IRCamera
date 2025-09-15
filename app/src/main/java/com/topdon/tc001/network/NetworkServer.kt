package com.topdon.tc001.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

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

    private val _messageFlow = MutableSharedFlow<JSONObject>()
    val messageFlow: SharedFlow<JSONObject> = _messageFlow.asSharedFlow()

    private val _connectionStateFlow = MutableStateFlow(false)
    val connectionStateFlow: StateFlow<Boolean> = _connectionStateFlow.asStateFlow()

    private var serverJob: Job? = null
    private var messageListenerJob: Job? = null

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

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Stopping TCP server")

                isRunning.set(false)
                isClientConnected.set(false)
                _connectionStateFlow.value = false

                serverJob?.cancel()
                messageListenerJob?.cancel()

                outputStream?.close()
                inputStream?.close()
                clientSocket?.close()

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

    suspend fun sendMessage(message: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get() || outputStream == null) {
                    Log.w(TAG, "No client connected, cannot send message")
                    return@withContext false
                }

                val messageData = message.toString().toByteArray(Charsets.UTF_8)

                outputStream!!.writeInt(messageData.size)
                outputStream!!.write(messageData)
                outputStream!!.flush()

                Log.d(TAG, "Sent message to PC: $message")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to PC", e)

                disconnectClient()
                return@withContext false
            }
        }
    }

    private suspend fun acceptConnections() {
        while (isRunning.get() && !serverJob?.isCancelled!!) {
            try {
                Log.i(TAG, "Waiting for PC Controller connection...")

                val socket = serverSocket?.accept()
                if (socket != null && isRunning.get()) {
                    Log.i(TAG, "PC Controller connected from ${socket.remoteSocketAddress}")

                    disconnectClient()

                    clientSocket = socket
                    outputStream = DataOutputStream(socket.getOutputStream())
                    inputStream = DataInputStream(socket.getInputStream())

                    isClientConnected.set(true)
                    _connectionStateFlow.value = true

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

    private suspend fun listenForMessages() {
        while (isClientConnected.get() && isRunning.get() && !messageListenerJob?.isCancelled!!) {
            try {
                val message = receiveMessage()
                if (message != null) {
                    _messageFlow.emit(message)
                } else {

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

        disconnectClient()
    }

    private suspend fun receiveMessage(): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val input = inputStream ?: return@withContext null

                val messageLength = input.readInt()

                if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
                    Log.e(TAG, "Invalid message length: $messageLength")
                    return@withContext null
                }

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

    fun isRunning(): Boolean = isRunning.get()

    fun isClientConnected(): Boolean = isClientConnected.get()

    suspend fun cleanup() {
        stop()
        serverScope.cancel()
        Log.i(TAG, "Network server cleaned up")
    }
}
