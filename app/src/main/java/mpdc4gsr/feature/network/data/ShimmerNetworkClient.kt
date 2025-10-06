package mpdc4gsr.feature.network.data

import android.net.TrafficStats
import android.os.Process
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.*
import mpdc4gsr.core.data.model.GSRSample
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ShimmerNetworkClient(
    private val serverHost: String = "192.168.1.100",
    private val serverPort: Int = 8888
) {
    companion object {
        private const val TAG = "ShimmerNetworkClient"
        private const val CONNECTION_TIMEOUT_MS = 5000
        private const val RECONNECT_DELAY_MS = 3000L
    }

    private var socket: Socket? = null
    private var outputStream: PrintWriter? = null
    private var inputStream: BufferedReader? = null
    private val isConnected = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    private val networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isConnected.get()) {
                AppLogger.i(TAG, "Already connected to server")
                return@withContext true
            }

            AppLogger.i(TAG, "Connecting to PC Controller at $serverHost:$serverPort")

            TrafficStats.setThreadStatsTag(Process.myTid())

            socket = Socket()
            socket?.connect(
                java.net.InetSocketAddress(serverHost, serverPort),
                CONNECTION_TIMEOUT_MS
            )

            socket?.let { TrafficStats.tagSocket(it) }

            outputStream = PrintWriter(socket?.getOutputStream()!!, true)
            inputStream = BufferedReader(InputStreamReader(socket?.getInputStream()!!))

            isConnected.set(true)
            isRunning.set(true)

            startMessageListener()

            startHeartbeat()

            AppLogger.i(TAG, "Connected to PC Controller successfully")
            withContext(Dispatchers.Main) {
                onConnected?.invoke()
            }

            return@withContext true

        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to connect to PC Controller: ${e.message}")
            cleanup()
            withContext(Dispatchers.Main) {
                onError?.invoke("Connection failed: ${e.message}")
            }
            return@withContext false
        }
    }

    fun disconnect() {
        networkScope.launch {
            try {
                AppLogger.i(TAG, "Disconnecting from PC Controller")
                cleanup()

                withContext(Dispatchers.Main) {
                    onDisconnected?.invoke()
                }

            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during disconnect: ${e.message}")
            }
        }
    }

    fun sendGSRSample(sample: GSRSample, sequenceNumber: Long) {
        if (!isConnected.get()) return

        networkScope.launch {
            try {
                val message = JSONObject().apply {
                    put("type", "gsr_sample")
                    put("timestamp_ms", sample.timestamp)
                    put("gsr_microsiemens", sample.gsrMicrosiemens)
                    put("raw_value", sample.gsrRaw)
                    put("resistance_kohm", sample.resistanceOhms / 1000.0)
                    put("sample_sequence", sequenceNumber)
                }

                sendMessage(message)

            } catch (e: Exception) {
                AppLogger.w(TAG, "Error sending GSR sample: ${e.message}")
            }
        }
    }

    fun sendRecordingStart(sessionId: String) {
        networkScope.launch {
            try {
                val message = JSONObject().apply {
                    put("type", "recording_start")
                    put("session_id", sessionId)
                    put("timestamp_ms", System.currentTimeMillis())
                }

                sendMessage(message)
                AppLogger.i(TAG, "Sent recording start notification")

            } catch (e: Exception) {
                AppLogger.w(TAG, "Error sending recording start: ${e.message}")
            }
        }
    }

    fun sendRecordingStop(sessionId: String, sampleCount: Long) {
        networkScope.launch {
            try {
                val message = JSONObject().apply {
                    put("type", "recording_stop")
                    put("session_id", sessionId)
                    put("timestamp_ms", System.currentTimeMillis())
                    put("total_samples", sampleCount)
                }

                sendMessage(message)
                AppLogger.i(TAG, "Sent recording stop notification")

            } catch (e: Exception) {
                AppLogger.w(TAG, "Error sending recording stop: ${e.message}")
            }
        }
    }

    fun sendSyncMarker(markerType: String, metadata: Map<String, String> = emptyMap()) {
        networkScope.launch {
            try {
                val message = JSONObject().apply {
                    put("type", "sync_marker")
                    put("marker_type", markerType)
                    put("timestamp_ms", System.currentTimeMillis())
                    put("metadata", JSONObject(metadata))
                }

                sendMessage(message)
                AppLogger.i(TAG, "Sent sync marker: $markerType")

            } catch (e: Exception) {
                AppLogger.w(TAG, "Error sending sync marker: ${e.message}")
            }
        }
    }

    private fun sendMessage(message: JSONObject) {
        try {
            outputStream?.let { out ->
                val messageStr = message.toString() + "\n"
                out.print(messageStr)
                out.flush()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error sending message: ${e.message}")

            handleConnectionError(e)
        }
    }

    private fun startMessageListener() {
        connectionJob = networkScope.launch {
            try {
                while (isRunning.get() && isConnected.get()) {
                    val input = inputStream
                    if (input != null) {
                        val line = input.readLine()
                        if (line != null) {
                            processServerMessage(line)
                        } else {
                            AppLogger.w(TAG, "Server closed connection")
                            return@launch
                        }
                    } else {
                        return@launch
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Message listener error: ${e.message}")
                handleConnectionError(e)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob = networkScope.launch {
            while (isRunning.get() && isConnected.get()) {
                try {
                    delay(30000)

                    val heartbeat = JSONObject().apply {
                        put("type", "heartbeat")
                        put("timestamp_ms", System.currentTimeMillis())
                    }

                    sendMessage(heartbeat)

                } catch (e: Exception) {
                    AppLogger.w(TAG, "Heartbeat error: ${e.message}")
                    break
                }
            }
        }
    }

    private fun processServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")

            when (type) {
                "connection_ack" -> {
                    AppLogger.i(TAG, "Received connection acknowledgment from PC Controller")
                }

                "sync_request" -> {
                    AppLogger.i(TAG, "Received sync request from PC Controller")

                }

                else -> {
                    AppLogger.d(TAG, "Received message: $type")
                }
            }

        } catch (e: Exception) {
            AppLogger.w(TAG, "Error processing server message: ${e.message}")
        }
    }

    private fun handleConnectionError(error: Exception) {
        AppLogger.w(TAG, "Connection error: ${error.message}")

        if (isRunning.get()) {
            cleanup()

            networkScope.launch {
                delay(RECONNECT_DELAY_MS)
                if (isRunning.get()) {
                    AppLogger.i(TAG, "Attempting to reconnect...")
                    connect()
                }
            }
        }
    }

    private fun cleanup() {
        isConnected.set(false)
        isRunning.set(false)

        connectionJob?.cancel()
        heartbeatJob?.cancel()
        networkScope.cancel()

        try {
            socket?.let { TrafficStats.untagSocket(it) }
            outputStream?.close()
            inputStream?.close()
            socket?.close()
            TrafficStats.clearThreadStatsTag()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error during cleanup: ${e.message}")
        }

        outputStream = null
        inputStream = null
        socket = null

        onConnected = null
        onDisconnected = null
        onError = null
    }

    fun isConnected(): Boolean = isConnected.get()

    fun getConnectionStatus(): String {
        return when {
            isConnected.get() -> "Connected to $serverHost:$serverPort"
            isRunning.get() -> "Connecting..."
            else -> "Disconnected"
        }
    }

    fun updateServerAddress(host: String, port: Int = serverPort): ShimmerNetworkClient {
        return ShimmerNetworkClient(host, port)
    }
}
