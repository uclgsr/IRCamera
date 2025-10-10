package mpdc4gsr.feature.connectivity.data

import android.content.Context
import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.AppLogger
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
    companion object {}


    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputWriter: BufferedWriter? = null
    private var inputReader: BufferedReader? = null
    private var binaryOutputStream: DataOutputStream? = null

    private val isRunning = AtomicBoolean(false)
    private val isClientConnected = AtomicBoolean(false)

    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _messageFlow = MutableSharedFlow<Protocol.ProtocolMessage>(extraBufferCapacity = 16)
    val messageFlow: SharedFlow<Protocol.ProtocolMessage> = _messageFlow.asSharedFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private var acceptJob: Job? = null
    private var messageListenerJob: Job? = null

    private val deviceId = "android_${android.os.Build.MODEL.replace(" ", "_")}"
    private val supportedSensors = listOf("RGB", "THERMAL", "GSR")

    suspend fun start(): Boolean =
        withContext(Dispatchers.IO) {
            if (isRunning.get()) {
                return@withContext true
            }

            try {
                TrafficStats.setThreadStatsTag(Process.myTid())
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }

                isRunning.set(true)
                acceptJob =
                    serverScope.launch {
                        acceptConnections()
                    }

                true
            } catch (e: Exception) {
                AppLogger.e("NetworkServer", "Failed to start server", e)
                stopInternal()
                false
            } finally {
                TrafficStats.clearThreadStatsTag()
            }
        }


    suspend fun stop() {
        withContext(Dispatchers.IO) {
            stopInternal()
        }
    }


    suspend fun sendMessage(message: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get()) {
                    return@withContext false
                }

                outputWriter?.apply {
                    write(message)
                    newLine()
                    flush()
                } ?: false
            } catch (e: Exception) {
                AppLogger.e("NetworkServer", "Failed to send message", e)
                disconnectClient()
                false
            }
        }


    suspend fun sendBinaryData(header: String, data: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!isClientConnected.get()) {
                    return@withContext false
                }

                outputWriter?.apply {
                    write(header)
                    newLine()
                    flush()
                }

                binaryOutputStream?.apply {
                    writeInt(data.size)
                    write(data)
                    flush()
                    true
                } ?: false
            } catch (e: Exception) {
                AppLogger.e("NetworkServer", "Failed to send binary data", e)
                disconnectClient()
                false
            }
        }


    private suspend fun acceptConnections() {
        while (isRunning.get()) {
            try {
                val socket = serverSocket?.accept() ?: continue
                TrafficStats.tagSocket(socket)
                prepareNewClient(socket)
            } catch (e: SocketException) {
                if (isRunning.get()) {
                    AppLogger.w("NetworkServer", "Socket exception while accepting connections", e)
                }

                break
            } catch (e: Exception) {
                AppLogger.e("NetworkServer", "Unexpected error while accepting connections", e)
                delay(1_000)
            }
        }
    }


    private fun prepareNewClient(socket: Socket) {
        disconnectClient()
        clientSocket = socket
        outputWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
        inputReader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        binaryOutputStream = DataOutputStream(socket.getOutputStream())
        isClientConnected.set(true)
        _connectionState.value = true

        sendWelcomeMessage()

        messageListenerJob =
            serverScope.launch {
                listenForMessages()
            }
    }


    private fun sendWelcomeMessage() {
        serverScope.launch {
            sendMessage(Protocol.createHelloMessage(deviceId, supportedSensors))
        }
    }


    private suspend fun listenForMessages() {
        while (isClientConnected.get()) {
            val message = receiveMessage() ?: break
            try {
                val protocolMessage = Protocol.parseMessage(message)
                if (protocolMessage != null) {
                    _messageFlow.emit(protocolMessage)
                } else {
                    AppLogger.w("NetworkServer", "Received invalid protocol message: $message")
                }
            } catch (e: Exception) {
                AppLogger.e("NetworkServer", "Failed to handle incoming message", e)
            }
        }

        disconnectClient()
    }


    private suspend fun receiveMessage(): String? =
        withContext(Dispatchers.IO) {
            try {
                inputReader?.readLine()
            } catch (e: Exception) {
                AppLogger.e("NetworkServer", "Failed to read message", e)
                null
            }
        }


    private fun disconnectClient() {
        if (!isClientConnected.compareAndSet(true, false)) {
            return
        }


        _connectionState.value = false
        messageListenerJob?.cancel()
        messageListenerJob = null

        try {
            clientSocket?.let { TrafficStats.untagSocket(it) }

            outputWriter?.close()
            inputReader?.close()
            binaryOutputStream?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            AppLogger.e("NetworkServer", "Error while disconnecting client", e)
        } finally {
            outputWriter = null
            inputReader = null
            binaryOutputStream = null
            clientSocket = null
        }
    }


    private fun stopInternal() {
        if (!isRunning.compareAndSet(true, false)) {
            return
        }


        acceptJob?.cancel()
        acceptJob = null
        disconnectClient()

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            AppLogger.e("NetworkServer", "Error while closing server socket", e)
        } finally {
            serverSocket = null
        }
    }


    fun isClientConnected(): Boolean = isClientConnected.get()
}
