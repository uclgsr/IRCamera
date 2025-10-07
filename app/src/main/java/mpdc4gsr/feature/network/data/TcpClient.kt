package mpdc4gsr.feature.network.data

import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.utils.AppLogger
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class TcpClient(
    private val serverHost: String,
    private val serverPort: Int
) : CommandConnection {
    companion object {
        private const val TAG = "TcpClient"
        private const val CONNECTION_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 30000
    }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readerJob: Job? = null
    private val _connectionState = MutableStateFlow(CommandConnection.ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CommandConnection.ConnectionState> =
        _connectionState.asStateFlow()
    private var messageCallback: ((String) -> Unit)? = null
    private var connectionCallback: ((CommandConnection.ConnectionState) -> Unit)? = null
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isConnected()) {
                AppLogger.i(TAG, "Already connected to $serverHost:$serverPort")
                return@withContext true
            }
            AppLogger.i(TAG, "Connecting to PC server at $serverHost:$serverPort")
            _connectionState.value = CommandConnection.ConnectionState.CONNECTING
            connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTING)
            TrafficStats.setThreadStatsTag(Process.myTid())
            socket = Socket().apply {
                soTimeout = READ_TIMEOUT_MS
                tcpNoDelay = true
            }
            socket?.connect(InetSocketAddress(serverHost, serverPort), CONNECTION_TIMEOUT_MS)
            socket?.let { TrafficStats.tagSocket(it) }
            val inputStream = socket?.getInputStream()
            val outputStream = socket?.getOutputStream()
            if (inputStream != null && outputStream != null) {
                reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
                _connectionState.value = CommandConnection.ConnectionState.CONNECTED
                connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTED)
                // Start reader thread
                startReaderLoop()
                AppLogger.i(TAG, "Successfully connected to PC server")
                return@withContext true
            } else {
                throw IOException("Failed to get socket streams")
            }
        } catch (e: SocketTimeoutException) {
            AppLogger.e(TAG, "Connection timeout to $serverHost:$serverPort")
            handleConnectionError("Connection timeout")
            return@withContext false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to connect to $serverHost:$serverPort", e)
            handleConnectionError("Connection failed: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentWriter = writer
            if (currentWriter != null && isConnected()) {
                currentWriter.write(message)
                currentWriter.write("\n")
                currentWriter.flush()
                AppLogger.d(TAG, "Sent message: $message")
                return@withContext true
            } else {
                AppLogger.w(TAG, "Cannot send message - not connected")
                return@withContext false
            }
        } catch (e: IOException) {
            AppLogger.e(TAG, "Failed to send message: $message", e)
            handleConnectionError("Send failed: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        AppLogger.i(TAG, "Disconnecting from PC server")
        // Cancel reader job first to stop any ongoing reads
        readerJob?.cancel()
        readerJob = null
        // Close resources in proper order: writer, reader, then socket
        try {
            writer?.let { w ->
                try {
                    w.flush()
                    w.close()
                } catch (e: IOException) {
                    AppLogger.w(TAG, "Error closing writer", e)
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error flushing/closing writer", e)
        }
        try {
            reader?.close()
        } catch (e: IOException) {
            AppLogger.w(TAG, "Error closing reader", e)
        }
        try {
            socket?.let { s ->
                TrafficStats.untagSocket(s)
                if (!s.isClosed) {
                    s.close()
                }
            }
            TrafficStats.clearThreadStatsTag()
        } catch (e: IOException) {
            AppLogger.w(TAG, "Error closing socket", e)
        }
        // Clear all references
        writer = null
        reader = null
        socket = null
        _connectionState.value = CommandConnection.ConnectionState.DISCONNECTED
        connectionCallback?.invoke(CommandConnection.ConnectionState.DISCONNECTED)
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true && !socket!!.isClosed && _connectionState.value == CommandConnection.ConnectionState.CONNECTED
    }

    override fun setMessageCallback(callback: (String) -> Unit) {
        messageCallback = callback
    }

    override fun setConnectionCallback(callback: (CommandConnection.ConnectionState) -> Unit) {
        connectionCallback = callback
    }

    override fun cleanup() {
        clientScope.launch {
            disconnect()
        }
        clientScope.cancel()
        messageCallback = null
        connectionCallback = null
    }

    private fun startReaderLoop() {
        readerJob = clientScope.launch {
            val currentReader = reader ?: return@launch
            try {
                while (isActive && isConnected()) {
                    try {
                        val message = currentReader.readLine()
                        if (message != null) {
                            AppLogger.d(TAG, "Received message: $message")
                            messageCallback?.invoke(message)
                        } else {
                            AppLogger.w(TAG, "Server closed connection")
                            break
                        }
                    } catch (e: SocketTimeoutException) {
                        // Read timeout is normal, continue reading
                        continue
                    } catch (e: SocketException) {
                        if (isActive) {
                            AppLogger.w(TAG, "Socket exception in reader loop", e)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    AppLogger.e(TAG, "Error in reader loop", e)
                    handleConnectionError("Reader error: ${e.message}")
                }
            }
            if (isActive) {
                handleConnectionError("Server disconnected")
            }
        }
    }

    private fun handleConnectionError(errorMessage: String) {
        AppLogger.w(TAG, "Connection error: $errorMessage")
        _connectionState.value = CommandConnection.ConnectionState.ERROR
        connectionCallback?.invoke(CommandConnection.ConnectionState.ERROR)
        clientScope.launch {
            disconnect()
        }
    }
}