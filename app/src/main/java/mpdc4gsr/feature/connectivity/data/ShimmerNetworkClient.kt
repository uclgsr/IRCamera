package mpdc4gsr.feature.connectivity.data

import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.AppLogger
import mpdc4gsr.gsr.model.GsrSample
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ShimmerNetworkClient(
    private val serverHost: String = "192.168.1.100",
    private val serverPort: Int = 8888,
) {
    companion object {
        private const val CONNECTION_TIMEOUT_MS = 5_000
        private const val RECONNECT_DELAY_MS = 3_000L
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
    }


    private var socket: Socket? = null
    private var outputWriter: PrintWriter? = null
    private var inputReader: BufferedReader? = null

    private val connected = AtomicBoolean(false)
    private val running = AtomicBoolean(false)

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerJob: Job? = null
    private var heartbeatJob: Job? = null

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    suspend fun connect(): Boolean =
        withContext(Dispatchers.IO) {
            if (connected.get()) {
                return@withContext true
            }


            try {
                TrafficStats.setThreadStatsTag(Process.myTid())
                socket = Socket().apply {
                    connect(InetSocketAddress(serverHost, serverPort), CONNECTION_TIMEOUT_MS)
                    TrafficStats.tagSocket(this)
                }

                outputWriter = PrintWriter(socket?.getOutputStream(), true)
                inputReader = BufferedReader(InputStreamReader(socket?.getInputStream()))

                connected.set(true)
                running.set(true)

                listenerJob = clientScope.launch { listenForMessages() }

                heartbeatJob = clientScope.launch { heartbeatLoop() }


                withContext(Dispatchers.Main) {
                    onConnected?.invoke()
                }


                true
            } catch (e: Exception) {
                handleConnectionFailure("Connection failed", e)
                false
            }
        }


    fun disconnect() {
        clientScope.launch {
            stopInternal()
            withContext(Dispatchers.Main) {
                onDisconnected?.invoke()
            }
        }
    }


    fun sendGsrSample(sample: GsrSample, sequenceNumber: Long) {
        sendJson(
            JSONObject()
                .put("type", "gsr_sample")
                .put("timestamp_ms", sample.timestampMillis)
                .put("gsr_microsiemens", sample.gsrMicrosiemens)
                .put("raw_value", sample.gsrRaw)
                .put("resistance_kohm", (sample.resistanceOhms ?: 0.0) / 1000.0)
                .put("sample_sequence", sequenceNumber),
        )
    }


    fun sendRecordingStart(sessionId: String) {
        sendJson(
            JSONObject()
                .put("type", "recording_start")
                .put("session_id", sessionId)
                .put("timestamp_ms", System.currentTimeMillis()),
        )
    }


    fun sendRecordingStop(sessionId: String, sampleCount: Long) {
        sendJson(
            JSONObject()
                .put("type", "recording_stop")
                .put("session_id", sessionId)
                .put("timestamp_ms", System.currentTimeMillis())
                .put("sample_count", sampleCount),
        )
    }


    fun sendStatus(status: String) {
        sendJson(
            JSONObject()
                .put("type", "status_update")
                .put("status", status)
                .put("timestamp_ms", System.currentTimeMillis()),
        )
    }


    private fun sendJson(json: JSONObject) {
        if (!connected.get())
            return
        clientScope.launch {
            try {
                outputWriter?.apply {
                    print(json.toString())
                    print("\n")
                    flush()
                }
            } catch (e: Exception) {
                handleConnectionFailure("Failed to send message", e)
            }
        }
    }


    private suspend fun listenForMessages() {
        try {
            while (running.get()) {
                val line = inputReader?.readLine() ?: break
                processServerMessage(line)
            }
        } catch (e: Exception) {
            handleConnectionFailure("Connection lost", e)
        } finally {
            stopInternal()
        }
    }


    private suspend fun heartbeatLoop() {
        while (running.get()) {
            try {
                delay(HEARTBEAT_INTERVAL_MS)
                sendJson(
                    JSONObject()
                        .put("type", "heartbeat")
                        .put("timestamp_ms", System.currentTimeMillis()),
                )
            } catch (e: Exception) {
                handleConnectionFailure("Heartbeat failed", e)
                break
            }
        }
    }


    private fun processServerMessage(message: String) {
        // Currently we only log messages from the server, but we keep the hook for future use.
        AppLogger.d("ShimmerNetworkClient", "Server message: $message")
    }


    private fun handleConnectionFailure(reason: String, throwable: Exception) {
        AppLogger.e("ShimmerNetworkClient", reason, throwable)
        clientScope.launch {
            stopInternal()
            withContext(Dispatchers.Main) {
                onError?.invoke("$reason: ${throwable.message}")
            }

            delay(RECONNECT_DELAY_MS)
            if (running.get()) {
                connect()
            }
        }
    }


    private fun stopInternal() {
        if (!connected.compareAndSet(true, false)) {
            return
        }


        running.set(false)

        listenerJob?.cancel()
        heartbeatJob?.cancel()
        clientScope.coroutineContext.cancelChildren()

        try {
            socket?.let { TrafficStats.untagSocket(it) }

            socket?.close()
            outputWriter?.close()
            inputReader?.close()
        } catch (e: Exception) {
            AppLogger.e("ShimmerNetworkClient", "Error while closing socket", e)
        } finally {
            socket = null
            outputWriter = null
            inputReader = null
            TrafficStats.clearThreadStatsTag()
        }
    }


    fun isConnected(): Boolean = connected.get()

    fun getConnectionStatus(): String =
        when {
            connected.get() -> "Connected to $serverHost:$serverPort"
            running.get() -> "Connecting..."
            else -> "Disconnected"
        }


    fun updateServerAddress(host: String, port: Int = serverPort): ShimmerNetworkClient =
        ShimmerNetworkClient(host, port).also {
            it.onConnected = onConnected
            it.onDisconnected = onDisconnected
            it.onError = onError
        }
}
