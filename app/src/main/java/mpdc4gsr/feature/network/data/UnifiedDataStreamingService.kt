package mpdc4gsr.feature.network.data

import android.content.Context
import kotlinx.coroutines.*
import mpdc4gsr.core.data.TimestampManager
import mpdc4gsr.core.data.TimestampRecord
import mpdc4gsr.core.sensors.gsr.model.GSRSample
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class UnifiedDataStreamingService(
    private val context: Context
) {
    companion object {
        private const val DEFAULT_PORT = 8888
        private const val BATCH_SIZE = 25
        private const val BATCH_TIMEOUT_MS = 50L
        private const val MAX_CLIENTS = 10
        private const val HEARTBEAT_INTERVAL_MS = 5000L
    }

    private val streamingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isStreaming = AtomicBoolean(false)
    private val connectedClients = mutableListOf<ClientHandler>()
    private val dataQueue = ConcurrentLinkedQueue<StreamingDataPacket>()
    private var serverSocket: ServerSocket? = null
    private var currentSessionId: String? = null
    private var sessionStartReference: TimestampRecord? = null
    private val packetsSent = AtomicLong(0)
    private val clientsConnected = AtomicLong(0)
    private val streamStartTime = AtomicLong(0)
    suspend fun startStreaming(sessionId: String, port: Int = DEFAULT_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isStreaming.get()) {
                    return@withContext true
                } currentSessionId = sessionId
                        sessionStartReference = TimestampManager.createTimestampRecord()
                streamStartTime.set(System.currentTimeMillis())
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                isStreaming.set(true)
                streamingScope.launch {
                    acceptClients()
                }
                streamingScope.launch {
                    processStreamingData()
                }
                streamingScope.launch {
                    distributeHeartbeats()
                } broadcastSessionSyncEvent (
                        "session_start", mapOf(
                "session_id" to sessionId,
                "timestamp_reference" to sessionStartReference!!.toCsvFormat()
                )
                )
                true
            } catch (e: Exception) {
                stopStreaming()
                false
            }
        }
    }

    suspend fun stopStreaming() {
        withContext(Dispatchers.IO) {
            try {
                currentSessionId?.let { sessionId ->
                    broadcastSessionSyncEvent(
                        "session_end", mapOf(
                            "session_id" to sessionId,
                            "duration_ms" to (System.currentTimeMillis() - streamStartTime.get()).toString(),
                            "packets_sent" to packetsSent.get().toString()
                        )
                    )
                }
                isStreaming.set(false)
                synchronized(connectedClients) {
                    connectedClients.forEach { client ->
                        client.disconnect()
                    }
                    connectedClients.clear()
                }
                serverSocket?.close()
                serverSocket = null
                dataQueue.clear()
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e(
                    "UnifiedDataStreamingService",
                    "Unexpected Exception in UnifiedDataStreamingService catch block",
                    e
                )
            }
        }
    }

    fun streamGSRData(gsrSample: GSRSample, timestampRecord: TimestampRecord) {
        if (!isStreaming.get()) return
        val packet = StreamingDataPacket(
            dataType = "GSR",
            timestamp = timestampRecord,
            data = JSONObject().apply {
                put("conductance_microsiemens", gsrSample.conductance)
                put("raw_adc", gsrSample.rawValue)
                put("ppg_value", 0) // Not available in this GSRSample model
                put("device_id", gsrSample.sessionId) // Use sessionId as device identifier
            }
        )
        dataQueue.offer(packet)
    }

    fun streamThermalData(
        frameNumber: Long,
        timestampRecord: TimestampRecord,
        minTemp: Float,
        maxTemp: Float,
        avgTemp: Float,
        centerTemp: Float
    ) {
        if (!isStreaming.get()) return
        val packet = StreamingDataPacket(
            dataType = "THERMAL",
            timestamp = timestampRecord,
            data = JSONObject().apply {
                put("frame_number", frameNumber)
                put("min_temp_c", minTemp)
                put("max_temp_c", maxTemp)
                put("avg_temp_c", avgTemp)
                put("center_temp_c", centerTemp)
            }
        )
        dataQueue.offer(packet)
    }

    fun streamRGBMetadata(
        frameNumber: Long,
        timestampRecord: TimestampRecord,
        filename: String,
        fileSize: Long
    ) {
        if (!isStreaming.get()) return
        val packet = StreamingDataPacket(
            dataType = "RGB",
            timestamp = timestampRecord,
            data = JSONObject().apply {
                put("frame_number", frameNumber)
                put("filename", filename)
                put("file_size", fileSize)
            }
        )
        dataQueue.offer(packet)
    }

    fun broadcastSyncMarker(
        markerType: String,
        timestampRecord: TimestampRecord,
        metadata: Map<String, String> = emptyMap()
    ) {
        if (!isStreaming.get()) return
        val syncPacket = JSONObject().apply {
            put("type", "SYNC_MARKER")
            put("marker_type", markerType)
            put("timestamp", timestampRecord.toCsvFormat())
            put("session_id", currentSessionId)
            put("metadata", JSONObject(metadata))
        }
        broadcastToClients(syncPacket.toString())
    }

    fun getStreamingStats(): StreamingStats {
        val uptime = if (streamStartTime.get() > 0) {
            (System.currentTimeMillis() - streamStartTime.get()) / 1000.0
        } else 0.0
        return StreamingStats(
            isActive = isStreaming.get(),
            connectedClients = synchronized(connectedClients) { connectedClients.size },
            packetsSent = packetsSent.get(),
            queueSize = dataQueue.size,
            uptimeSeconds = uptime,
            sessionId = currentSessionId
        )
    }

    private suspend fun acceptClients() {
        while (isStreaming.get()) {
            try {
                val socket = serverSocket?.accept()
                if (socket != null) {
                    val clientHandler = ClientHandler(socket)
                    synchronized(connectedClients) {
                        if (connectedClients.size < MAX_CLIENTS) {
                            connectedClients.add(clientHandler)
                            clientsConnected.incrementAndGet()
                            clientHandler.sendSessionInfo()
                        } else {
                            socket.close()
                        }
                    }
                }
            } catch (e: Exception) {
                if (isStreaming.get()) {
                }
            }
        }
    }

    private suspend fun processStreamingData() {
        val batch = mutableListOf<StreamingDataPacket>()
        while (isStreaming.get()) {
            try {
                val startTime = System.currentTimeMillis()
                while (batch.size < BATCH_SIZE &&
                    (System.currentTimeMillis() - startTime) < BATCH_TIMEOUT_MS
                ) {
                    val packet = dataQueue.poll()
                    if (packet != null) {
                        batch.add(packet)
                    } else {
                        delay(1)
                    }
                }
                if (batch.isNotEmpty()) {
                    val batchMessage = JSONObject().apply {
                        put("type", "DATA_BATCH")
                        put("session_id", currentSessionId)
                        put("batch_size", batch.size)
                        put("packets", JSONArray().apply {
                            batch.forEach { packet ->
                                put(JSONObject().apply {
                                    put("data_type", packet.dataType)
                                    put("timestamp", packet.timestamp.toCsvFormat())
                                    put("data", packet.data)
                                })
                            }
                        })
                    }
                    broadcastToClients(batchMessage.toString())
                    packetsSent.addAndGet(batch.size.toLong())
                    batch.clear()
                }
                delay(1)
            } catch (e: Exception) {
                delay(100)
            }
        }
    }

    private suspend fun distributeHeartbeats() {
        while (isStreaming.get()) {
            try {
                val heartbeat = JSONObject().apply {
                    put("type", "HEARTBEAT")
                    put("timestamp", TimestampManager.createTimestampRecord().toCsvFormat())
                    put("session_id", currentSessionId)
                    put("stats", JSONObject().apply {
                        val stats = getStreamingStats()
                        put("packets_sent", stats.packetsSent)
                        put("connected_clients", stats.connectedClients)
                        put("uptime_seconds", stats.uptimeSeconds)
                    })
                }
                broadcastToClients(heartbeat.toString())
                delay(HEARTBEAT_INTERVAL_MS)
            } catch (e: Exception) {
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun broadcastSessionSyncEvent(eventType: String, metadata: Map<String, String>) {
        val syncEvent = JSONObject().apply {
            put("type", "SESSION_SYNC_EVENT")
            put("event_type", eventType)
            put("timestamp", TimestampManager.createTimestampRecord().toCsvFormat())
            put("session_id", currentSessionId)
            put("metadata", JSONObject(metadata))
        }
        broadcastToClients(syncEvent.toString())
    }

    private fun broadcastToClients(message: String) {
        synchronized(connectedClients) {
            val disconnectedClients = mutableListOf<ClientHandler>()
            connectedClients.forEach { client ->
                if (!client.sendMessage(message)) {
                    disconnectedClients.add(client)
                }
            }
            disconnectedClients.forEach { client ->
                connectedClients.remove(client)
                client.disconnect()
            }
        }
    }

    private inner class ClientHandler(private val socket: Socket) {
        private val writer: PrintWriter = PrintWriter(socket.getOutputStream(), true)
        private val isConnected = AtomicBoolean(true)
        fun sendMessage(message: String): Boolean {
            return try {
                if (isConnected.get() && !socket.isClosed) {
                    writer.println(message)
                    !writer.checkError()
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        fun sendSessionInfo() {
            val sessionInfo = JSONObject().apply {
                put("type", "SESSION_INFO")
                put("session_id", currentSessionId)
                put("timestamp_reference", sessionStartReference?.toCsvFormat())
                put("streaming_started", streamStartTime.get())
            }
            sendMessage(sessionInfo.toString())
        }

        fun disconnect() {
            isConnected.set(false)
            try {
                writer.close()
                socket.close()
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e(
                    "UnifiedDataStreamingService",
                    "Unexpected Exception in UnifiedDataStreamingService catch block",
                    e
                )
            }
        }
    }

    data class StreamingDataPacket(
        val dataType: String,
        val timestamp: TimestampRecord,
        val data: JSONObject
    )

    data class StreamingStats(
        val isActive: Boolean,
        val connectedClients: Int,
        val packetsSent: Long,
        val queueSize: Int,
        val uptimeSeconds: Double,
        val sessionId: String?
    )
}
