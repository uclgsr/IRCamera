package com.mpdc4gsr.gsr.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class QualityOfServiceManager(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "QoSManager"
        private const val BANDWIDTH_MONITOR_INTERVAL = 2000L
        private const val CONGESTION_THRESHOLD = 0.8f
        private const val PRIORITY_QUEUE_SIZE = 1000
        private const val ADAPTIVE_BATCH_MIN = 10
        private const val ADAPTIVE_BATCH_MAX = 200
        private const val NETWORK_LATENCY_SAMPLES = 10
    }

    private val qosJob = SupervisorJob()
    private val qosScope = CoroutineScope(Dispatchers.IO + qosJob)
    private val isMonitoring = AtomicBoolean(false)
    private val currentBandwidth = AtomicLong(0)
    private val networkLatency = AtomicLong(0)
    private val packetLossRate = AtomicLong(0)
    private val criticalQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private val highPriorityQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private val normalPriorityQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private val lowPriorityQueue = ConcurrentLinkedQueue<QoSDataPacket>()
    private var adaptiveBatchSize = 50
    private var compressionLevel = CompressionLevel.MEDIUM
    private var currentNetworkTier = NetworkTier.MEDIUM

    data class QoSDataPacket(
        val data: ByteArray,
        val dataType: DataType,
        val priority: Priority,
        val timestamp: Long,
        val sessionId: String,
        val metadata: Map<String, String> = emptyMap(),
    )

    enum class DataType(val typeName: String) {
        GSR("gsr"),
        THERMAL("thermal"),
        VIDEO_METADATA("video_metadata"),
        CONTROL_MESSAGE("control"),
        FILE_CHUNK("file_chunk"),
        HEARTBEAT("heartbeat"),
    }

    enum class Priority(val level: Int) {
        CRITICAL(4),
        HIGH(3),
        NORMAL(2),
        LOW(1),
    }

    enum class CompressionLevel(val factor: Float) {
        NONE(1.0f),
        LOW(0.9f),
        MEDIUM(0.7f),
        HIGH(0.5f),
        MAXIMUM(0.3f),
    }

    enum class NetworkTier {
        POOR,
        LOW,
        MEDIUM,
        HIGH,
        EXCELLENT,
    }

    data class NetworkQualityMetrics(
        val bandwidth: Long,
        val latency: Long,
        val packetLoss: Float,
        val networkTier: NetworkTier,
        val recommendedBatchSize: Int,
        val recommendedCompression: CompressionLevel,
        val congestionLevel: Float,
    )

    suspend fun startQoSMonitoring() =
        withContext(Dispatchers.IO) {
            if (isMonitoring.getAndSet(true)) {
                Log.w(TAG, "QoS monitoring already active")
                return@withContext
            }
            Log.d(TAG, "Starting QoS monitoring")
            startBandwidthMonitoring()
            startLatencyMonitoring()
            startAdaptiveProcessing()
            startPriorityQueueProcessor()
        }

    private fun startBandwidthMonitoring() {
        qosScope.launch {
            while (isMonitoring.get()) {
                val bandwidth = measureBandwidth()
                currentBandwidth.set(bandwidth)
                updateNetworkTier(bandwidth)
                adjustCompressionLevel(bandwidth)
                delay(BANDWIDTH_MONITOR_INTERVAL)
            }
        }
    }

    private fun startLatencyMonitoring() {
        qosScope.launch {
            while (isMonitoring.get()) {
                val latency = measureNetworkLatency()
                networkLatency.set(latency)
                delay(5000L)
            }
        }
    }

    private suspend fun measureBandwidth(): Long =
        withContext(Dispatchers.IO) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return@withContext when {
                networkCapabilities == null -> 0L
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    measureWiFiBandwidth()
                }

                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    measureCellularBandwidth()
                }

                else -> 1024 * 1024L
            }
        }

    @Suppress("DEPRECATION")
    private fun measureWiFiBandwidth(): Long {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val rssi = wifiInfo.rssi
        val linkSpeed = wifiInfo.linkSpeed
        val signalQuality =
            when {
                rssi >= -50 -> 1.0f
                rssi >= -60 -> 0.8f
                rssi >= -70 -> 0.6f
                rssi >= -80 -> 0.4f
                else -> 0.2f
            }
        return (linkSpeed * 1024 * 1024 / 8 * signalQuality).toLong()
    }

    private fun measureCellularBandwidth(): Long {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return when {
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true -> {
                2 * 1024 * 1024L
            }

            else -> 512 * 1024L
        }
    }

    private suspend fun measureNetworkLatency(): Long =
        withContext(Dispatchers.IO) {
            val samples = mutableListOf<Long>()
            repeat(NETWORK_LATENCY_SAMPLES) {
                val startTime = System.currentTimeMillis()
                try {
                    val pingMessage =
                        JSONObject().apply {
                            put("type", "qos_ping")
                            put("timestamp", startTime)
                        }
                    networkClient.sendMessage(pingMessage)
                    val response = networkClient.waitForResponse("qos_pong", 2000L)
                    val endTime = System.currentTimeMillis()
                    val latency = endTime - startTime
                    samples.add(latency)
                } catch (e: Exception) {
                    samples.add(2000L)
                }
                delay(100L)
            }
            samples.sorted()[samples.size / 2]
        }

    private fun updateNetworkTier(bandwidth: Long) {
        currentNetworkTier =
            when {
                bandwidth > 10 * 1024 * 1024L -> NetworkTier.EXCELLENT
                bandwidth > 2 * 1024 * 1024L -> NetworkTier.HIGH
                bandwidth > 500 * 1024L -> NetworkTier.MEDIUM
                bandwidth > 100 * 1024L -> NetworkTier.LOW
                else -> NetworkTier.POOR
            }
        Log.d(TAG, "Network tier updated: $currentNetworkTier (${bandwidth / 1024}KB/s)")
    }

    private fun adjustCompressionLevel(bandwidth: Long) {
        compressionLevel =
            when (currentNetworkTier) {
                NetworkTier.POOR -> CompressionLevel.MAXIMUM
                NetworkTier.LOW -> CompressionLevel.HIGH
                NetworkTier.MEDIUM -> CompressionLevel.MEDIUM
                NetworkTier.HIGH -> CompressionLevel.LOW
                NetworkTier.EXCELLENT -> CompressionLevel.NONE
            }
    }

    private fun startAdaptiveProcessing() {
        qosScope.launch {
            while (isMonitoring.get()) {
                adaptParameters()
                delay(BANDWIDTH_MONITOR_INTERVAL)
            }
        }
    }

    private fun adaptParameters() {
        val bandwidth = currentBandwidth.get()
        val latency = networkLatency.get()
        val utilization = calculateBandwidthUtilization()
        adaptiveBatchSize =
            when {
                bandwidth > 5 * 1024 * 1024L && latency < 50L -> ADAPTIVE_BATCH_MAX
                bandwidth > 1 * 1024 * 1024L && latency < 100L -> (ADAPTIVE_BATCH_MAX * 0.7).toInt()
                bandwidth > 500 * 1024L -> (ADAPTIVE_BATCH_MAX * 0.5).toInt()
                else -> ADAPTIVE_BATCH_MIN
            }
        if (utilization > CONGESTION_THRESHOLD) {
            adaptiveBatchSize = (adaptiveBatchSize * 0.7).toInt()
        }
        Log.v(TAG, "Adapted batch size: $adaptiveBatchSize, utilization: $utilization")
    }

    private fun calculateBandwidthUtilization(): Float {
        val availableBandwidth = currentBandwidth.get()
        if (availableBandwidth <= 0) return 1.0f
        val usedBandwidth = calculateCurrentUsage()
        return (usedBandwidth.toFloat() / availableBandwidth.toFloat()).coerceAtMost(1.0f)
    }

    private fun calculateCurrentUsage(): Long {
        val queueSize = getTotalQueueSize()
        return queueSize * 100L
    }

    fun queueData(
        data: ByteArray,
        dataType: DataType,
        priority: Priority,
        sessionId: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        val packet =
            QoSDataPacket(
                data = data,
                dataType = dataType,
                priority = priority,
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                metadata = metadata,
            )
        val targetQueue =
            when (priority) {
                Priority.CRITICAL -> criticalQueue
                Priority.HIGH -> highPriorityQueue
                Priority.NORMAL -> normalPriorityQueue
                Priority.LOW -> lowPriorityQueue
            }
        while (targetQueue.size >= PRIORITY_QUEUE_SIZE) {
            val dropped = targetQueue.poll()
            Log.w(TAG, "Dropped packet due to queue overflow: ${dropped?.dataType}")
        }
        targetQueue.offer(packet)
    }

    private fun startPriorityQueueProcessor() {
        qosScope.launch {
            while (isMonitoring.get()) {
                processPriorityQueues()
                delay(50L)
            }
        }
    }

    private suspend fun processPriorityQueues() {
        val batch = mutableListOf<QoSDataPacket>()
        val maxBatchSize = adaptiveBatchSize
        while (criticalQueue.isNotEmpty() && batch.size < maxBatchSize) {
            criticalQueue.poll()?.let { batch.add(it) }
        }
        while (highPriorityQueue.isNotEmpty() && batch.size < maxBatchSize) {
            highPriorityQueue.poll()?.let { batch.add(it) }
        }
        while (normalPriorityQueue.isNotEmpty() && batch.size < maxBatchSize) {
            normalPriorityQueue.poll()?.let { batch.add(it) }
        }
        if (calculateBandwidthUtilization() < CONGESTION_THRESHOLD) {
            while (lowPriorityQueue.isNotEmpty() && batch.size < maxBatchSize) {
                lowPriorityQueue.poll()?.let { batch.add(it) }
            }
        }
        if (batch.isNotEmpty()) {
            sendBatch(batch)
        }
    }

    private suspend fun sendBatch(batch: List<QoSDataPacket>) {
        try {
            val compressedBatch = compressBatch(batch)
            val batchMessage = createBatchMessage(compressedBatch)
            networkClient.sendMessage(batchMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send batch", e)
            batch.filter { it.priority.level >= Priority.HIGH.level }
                .forEach { queueData(it.data, it.dataType, it.priority, it.sessionId, it.metadata) }
        }
    }

    private fun compressBatch(batch: List<QoSDataPacket>): List<QoSDataPacket> {
        if (compressionLevel == CompressionLevel.NONE) return batch
        return batch.map { packet ->
            when (packet.dataType) {
                DataType.GSR -> packet
                DataType.THERMAL -> compressThermalData(packet)
                DataType.VIDEO_METADATA -> compressVideoMetadata(packet)
                else -> packet
            }
        }
    }

    private fun compressThermalData(packet: QoSDataPacket): QoSDataPacket {
        return packet
    }

    private fun compressVideoMetadata(packet: QoSDataPacket): QoSDataPacket {
        return packet
    }

    private fun createBatchMessage(batch: List<QoSDataPacket>): JSONObject {
        return JSONObject().apply {
            put("type", "qos_batch")
            put("batch_size", batch.size)
            put("compression_level", compressionLevel.name)
            put("timestamp", System.currentTimeMillis())
        }
    }

    fun getNetworkQualityMetrics(): NetworkQualityMetrics {
        return NetworkQualityMetrics(
            bandwidth = currentBandwidth.get(),
            latency = networkLatency.get(),
            packetLoss = packetLossRate.get() / 100.0f,
            networkTier = currentNetworkTier,
            recommendedBatchSize = adaptiveBatchSize,
            recommendedCompression = compressionLevel,
            congestionLevel = calculateBandwidthUtilization(),
        )
    }

    fun getQueueStatistics(): QueueStatistics {
        return QueueStatistics(
            criticalQueueSize = criticalQueue.size,
            highPriorityQueueSize = highPriorityQueue.size,
            normalPriorityQueueSize = normalPriorityQueue.size,
            lowPriorityQueueSize = lowPriorityQueue.size,
            totalQueueSize = getTotalQueueSize(),
            adaptiveBatchSize = adaptiveBatchSize,
        )
    }

    data class QueueStatistics(
        val criticalQueueSize: Int,
        val highPriorityQueueSize: Int,
        val normalPriorityQueueSize: Int,
        val lowPriorityQueueSize: Int,
        val totalQueueSize: Int,
        val adaptiveBatchSize: Int,
    )

    private fun getTotalQueueSize(): Int {
        return criticalQueue.size + highPriorityQueue.size +
                normalPriorityQueue.size + lowPriorityQueue.size
    }

    fun stopQoSMonitoring() {
        isMonitoring.set(false)
        criticalQueue.clear()
        highPriorityQueue.clear()
        normalPriorityQueue.clear()
        lowPriorityQueue.clear()
        qosJob.cancel()
        Log.d(TAG, "QoS monitoring stopped")
    }
}
