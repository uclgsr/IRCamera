package mpdc4gsr.network

import android.content.Context
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
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class NetworkServer(
    private val context: Context,
    private val port: Int = Protocol.DEFAULT_PORT,
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
                if (isRunning.get()) {                    return@withContext true
                }                serverSocket = ServerSocket(port)
                isRunning.set(true)

                serverJob =
                    serverScope.launch {
                        acceptConnections()
                    }                return@withContext true
            } catch (e: Exception) {                isRunning.set(false)
                return@withContext false
            }
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {                isRunning.set(false)
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
                serverSocket = null            } catch (e: Exception) {            }
        }
    }

    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get() || outputWriter == null) {                    return@withContext false
                }

                outputWriter!!.write(message + "\n")
                outputWriter!!.flush()                return@withContext true
            } catch (e: Exception) {                disconnectClient()
                return@withContext false
            }
        }
    }

    suspend fun sendBinaryData(header: String, data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get() || outputWriter == null || binaryOutputStream == null) {                    return@withContext false
                }

                // Send text header first
                outputWriter!!.write(header + "\n")
                outputWriter!!.flush()

                // Send binary data with length prefix
                binaryOutputStream!!.writeInt(data.size)
                binaryOutputStream!!.write(data)
                binaryOutputStream!!.flush()                return@withContext true
            } catch (e: Exception) {                disconnectClient()
                return@withContext false
            }
        }
    }

    private suspend fun acceptConnections() {
        while (isRunning.get() && !serverJob?.isCancelled!!) {
            try {                val socket = serverSocket?.accept()
                if (socket != null && isRunning.get()) {                    disconnectClient()

                    clientSocket = socket
                    outputWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                    inputReader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
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
                if (isRunning.get()) {                } else {                }
                break
            } catch (e: Exception) {                delay(1000)
            }
        }
    }

    private suspend fun listenForMessages() {
        while (isClientConnected.get() && isRunning.get() && !messageListenerJob?.isCancelled!!) {
            try {
                val messageText = receiveMessage()
                if (messageText != null) {
                    val protocolMessage = Protocol.parseMessage(messageText)
                    if (protocolMessage != null) {
                        _messageFlow.emit(protocolMessage)
                    } else {                    }
                } else {
                    break
                }
            } catch (e: SocketException) {                break
            } catch (e: Exception) {                break
            }
        }

        disconnectClient()
    }

    private suspend fun receiveMessage(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val reader = inputReader ?: return@withContext null
                val line = reader.readLine()

                if (line != null) {                }

                return@withContext line
            } catch (e: Exception) {                return@withContext null
            }
        }
    }

    private fun disconnectClient() {
        if (isClientConnected.get()) {            isClientConnected.set(false)
            _connectionStateFlow.value = false

            messageListenerJob?.cancel()

            try {
                outputWriter?.close()
                inputReader?.close()
                binaryOutputStream?.close()
                clientSocket?.close()
            } catch (e: Exception) {            }

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
        serverScope.cancel()    }
}
