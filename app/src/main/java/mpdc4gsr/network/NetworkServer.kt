package mpdc4gsr.network

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
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class NetworkServer(
    private val context: Context,
    private val port: Int = Protocol.DEFAULT_SERVER_PORT,
) {
    companion object {
        private const val TAG = "NetworkServer"
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputWriter: BufferedWriter? = null
    private var inputReader: BufferedReader? = null
    private var binaryOutputStream: DataOutputStream? = null

    private val isRunning = AtomicBoolean(false)
    private val isClientConnected = AtomicBoolean(false)
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messageFlow = MutableSharedFlow<Protocol.ProtocolMessage>()
    val messageFlow: SharedFlow<Protocol.ProtocolMessage> = _messageFlow.asSharedFlow()

    private val _connectionStateFlow = MutableStateFlow(false)
    val connectionStateFlow: StateFlow<Boolean> = _connectionStateFlow.asStateFlow()

    private var serverJob: Job? = null
    private var messageListenerJob: Job? = null

    // Device information for HELLO message
    private val deviceId = "android_${android.os.Build.MODEL.replace(" ", "_")}"
    private val supportedSensors = listOf("RGB", "THERMAL", "GSR")

    suspend fun start(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isRunning.get()) {
                    Log.w(TAG, "Server already running")
                    return@withContext true
                }

                Log.i(TAG, "Starting TCP server on port $port")

                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                isRunning.set(true)

                serverJob =
                    serverScope.launch {
                        acceptConnections()
                    }

                Log.i(TAG, "TCP server started successfully on port $port")
                return@withContext true
            } catch (e: java.net.BindException) {
                Log.e(TAG, "Failed to start TCP server - port $port already in use", e)
                isRunning.set(false)
                return@withContext false
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

                outputWriter?.close()
                inputReader?.close()
                binaryOutputStream?.close()
                clientSocket?.close()

                serverSocket?.close()

                outputWriter = null
                inputReader = null
                binaryOutputStream = null
                clientSocket = null
                serverSocket = null

                Log.i(TAG, "TCP server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping TCP server", e)
            }
        }
    }

    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get() || outputWriter == null) {
                    Log.w(TAG, "No client connected, cannot send message")
                    return@withContext false
                }

                outputWriter!!.write(message + "\n")
                outputWriter!!.flush()

                Log.d(TAG, "Sent message to PC: $message")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to PC", e)
                disconnectClient()
                return@withContext false
            }
        }
    }

    suspend fun sendBinaryData(header: String, data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get() || outputWriter == null || binaryOutputStream == null) {
                    Log.w(TAG, "No client connected, cannot send binary data")
                    return@withContext false
                }

                // Send text header first
                outputWriter!!.write(header + "\n")
                outputWriter!!.flush()

                // Send binary data with length prefix
                binaryOutputStream!!.writeInt(data.size)
                binaryOutputStream!!.write(data)
                binaryOutputStream!!.flush()

                Log.d(TAG, "Sent binary data to PC: ${data.size} bytes")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending binary data to PC", e)
                disconnectClient()
                return@withContext false
            }
        }
    }

    private suspend fun acceptConnections() {
        while (isRunning.get() && serverJob?.isCancelled != true) {
            try {
                Log.i(TAG, "Waiting for PC Controller connection...")

                val socket = serverSocket?.accept()
                if (socket != null && isRunning.get()) {
                    Log.i(TAG, "PC Controller connected from ${socket.remoteSocketAddress}")

                    disconnectClient()

                    clientSocket = socket
                    outputWriter =
                        BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                    inputReader =
                        BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    binaryOutputStream = DataOutputStream(socket.getOutputStream())

                    isClientConnected.set(true)
                    _connectionStateFlow.value = true

                    // Send HELLO message immediately upon connection
                    sendMessage(Protocol.createHelloMessage(deviceId, supportedSensors))

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
                delay(1000)
            }
        }
    }

    private suspend fun listenForMessages() {
        while (isClientConnected.get() && isRunning.get() && messageListenerJob?.isCancelled != true) {
            try {
                val messageText = receiveMessage()
                if (messageText != null) {
                    val protocolMessage = Protocol.parseMessage(messageText)
                    if (protocolMessage != null) {
                        _messageFlow.emit(protocolMessage)
                    } else {
                        Log.w(TAG, "Failed to parse protocol message: $messageText")
                    }
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

    private suspend fun receiveMessage(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val reader = inputReader ?: return@withContext null
                val line = reader.readLine()

                if (line != null) {
                    Log.d(TAG, "Received message from PC: $line")
                }

                return@withContext line
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
                outputWriter?.close()
                inputReader?.close()
                binaryOutputStream?.close()
                clientSocket?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing client connection", e)
            }

            outputWriter = null
            inputReader = null
            binaryOutputStream = null
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
