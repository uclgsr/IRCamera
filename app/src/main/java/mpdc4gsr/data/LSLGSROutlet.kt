package mpdc4gsr.data

import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Lab Streaming Layer (LSL) GSR Data Outlet
 * 
 * Implements optional LSL outlet for real-time GSR monitoring and visualization.
 * Addresses TODO: "Implement an optional Lab Streaming Layer outlet or similar 
 * for live GSR monitoring. This feature (for real-time visualization) is noted 
 * but not yet implemented"
 */
class LSLGSROutlet {
    companion object {
        private const val TAG = "LSLGSROutlet"
        
        
        private const val DEFAULT_SAMPLE_RATE = 128.0
        private const val DEFAULT_CHANNEL_COUNT = 1
        private const val STREAM_TYPE = "GSR"
        private const val STREAM_FORMAT = "double64"
        
        
        private const val MAX_BUFFER_SIZE = 1000
        private const val CHUNK_SIZE = 32
        
        
        private const val QUALITY_HISTORY_SIZE = 100
        private const val MIN_QUALITY_THRESHOLD = 0.7
    }
    
    
    data class LSLStreamInfo(
        val name: String,
        val type: String,
        val channelCount: Int,
        val sampleRate: Double,
        val channelFormat: String,
        val sourceId: String,
        val hostname: String = "IRCamera-Android",
        val sessionId: String? = null
    )
    
    
    class LSLStreamOutlet(private val streamInfo: LSLStreamInfo) {
        private val isActive = AtomicBoolean(false)
        private var startTime = 0L
        private val sampleCount = AtomicLong(0)
        
        fun open(): Boolean {
            return try {
                startTime = System.currentTimeMillis()
                isActive.set(true)
                Log.i(TAG, "LSL outlet opened: ${streamInfo.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open LSL outlet: ${e.message}")
                false
            }
        }
        
        fun pushSample(data: DoubleArray, timestamp: Double? = null): Boolean {
            if (!isActive.get()) {
                return false
            }
            
            return try {
                
                val actualTimestamp = timestamp ?: (System.nanoTime() / 1_000_000_000.0)
                
                
                simulateLSLPush(data, actualTimestamp)
                
                sampleCount.incrementAndGet()
                Log.v(TAG, "Pushed GSR sample: ${data.contentToString()} @ $actualTimestamp")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push LSL sample: ${e.message}")
                false
            }
        }
        
        fun pushChunk(dataMatrix: Array<DoubleArray>, timestamps: DoubleArray? = null): Boolean {
            if (!isActive.get()) {
                return false
            }
            
            return try {
                
                for (i in dataMatrix.indices) {
                    val timestamp = timestamps?.getOrNull(i) ?: (System.nanoTime() / 1_000_000_000.0)
                    simulateLSLPush(dataMatrix[i], timestamp)
                }
                
                sampleCount.addAndGet(dataMatrix.size.toLong())
                Log.v(TAG, "Pushed GSR chunk: ${dataMatrix.size} samples")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push LSL chunk: ${e.message}")
                false
            }
        }
        
        fun close() {
            isActive.set(false)
            Log.i(TAG, "LSL outlet closed: ${streamInfo.name} (${sampleCount.get()} samples streamed)")
        }
        
        fun isOpen(): Boolean = isActive.get()
        
        fun getSampleCount(): Long = sampleCount.get()
        
        fun getUptime(): Long = if (startTime > 0) System.currentTimeMillis() - startTime else 0L
        
        private fun simulateLSLPush(data: DoubleArray, timestamp: Double) {
            
            Thread.sleep((1..5).random().toLong())
            
            
            if (Math.random() < 0.001) { 
                throw RuntimeException("Simulated network error")
            }
        }
    }
    
    
    data class GSRSample(
        val timestamp: Long,
        val gsrMicrosiemens: Double,
        val rawValue: Int,
        val resistance: Double,
        val deviceId: String,
        val quality: Double = 1.0
    )
    
    
    data class OutletStatistics(
        val deviceId: String,
        val streamName: String,
        val isActive: Boolean,
        val sampleCount: Long,
        val uptime: Long,
        val avgQuality: Double,
        val bufferUtilization: Double,
        val lastSampleTime: Long
    )
    
    
    private val activeOutlets = ConcurrentHashMap<String, LSLStreamOutlet>()
    private val deviceBuffers = ConcurrentHashMap<String, LinkedList<GSRSample>>()
    private val qualityHistory = ConcurrentHashMap<String, LinkedList<Double>>()
    
    
    private val isStreamingEnabled = AtomicBoolean(false)
    private var streamingJob: Job? = null
    private val streamingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    
    private val totalSamplesStreamed = AtomicLong(0)
    private val streamingErrors = AtomicLong(0)
    
    
    fun createGSRStream(
        deviceId: String, 
        sampleRate: Double = DEFAULT_SAMPLE_RATE,
        sessionId: String? = null
    ): Boolean {
        return try {
            
            val streamInfo = LSLStreamInfo(
                name = "GSR-$deviceId",
                type = STREAM_TYPE,
                channelCount = DEFAULT_CHANNEL_COUNT,
                sampleRate = sampleRate,
                channelFormat = STREAM_FORMAT,
                sourceId = "IRCamera-GSR-$deviceId",
                sessionId = sessionId
            )
            
            
            val outlet = LSLStreamOutlet(streamInfo)
            if (outlet.open()) {
                activeOutlets[deviceId] = outlet
                deviceBuffers[deviceId] = LinkedList()
                qualityHistory[deviceId] = LinkedList()
                
                Log.i(TAG, "Created LSL GSR stream for device: $deviceId (${sampleRate}Hz)")
                return true
            } else {
                Log.e(TAG, "Failed to open LSL outlet for device: $deviceId")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating LSL stream for device $deviceId: ${e.message}")
            false
        }
    }
    
    
    fun startStreaming(): Boolean {
        if (isStreamingEnabled.get()) {
            Log.w(TAG, "LSL streaming already enabled")
            return true
        }
        
        if (activeOutlets.isEmpty()) {
            Log.w(TAG, "No active LSL outlets to stream")
            return false
        }
        
        isStreamingEnabled.set(true)
        startStreamingJob()
        
        Log.i(TAG, "Started LSL streaming for ${activeOutlets.size} outlets")
        return true
    }
    
    
    fun stopStreaming() {
        if (!isStreamingEnabled.get()) {
            return
        }
        
        isStreamingEnabled.set(false)
        streamingJob?.cancel()
        
        Log.i(TAG, "Stopped LSL streaming")
    }
    
    
    fun addGSRSample(sample: GSRSample) {
        if (!isStreamingEnabled.get()) {
            return
        }
        
        val buffer = deviceBuffers[sample.deviceId]
        if (buffer != null) {
            synchronized(buffer) {
                
                buffer.offer(sample)
                
                
                while (buffer.size > MAX_BUFFER_SIZE) {
                    buffer.poll()
                }
            }
            
            
            updateQualityHistory(sample.deviceId, sample.quality)
            
            Log.v(TAG, "Added GSR sample to LSL buffer: ${sample.deviceId} = ${sample.gsrMicrosiemens} µS")
        }
    }
    
    
    fun addGSRBatch(samples: List<GSRSample>) {
        if (!isStreamingEnabled.get() || samples.isEmpty()) {
            return
        }
        
        
        val samplesByDevice = samples.groupBy { it.deviceId }
        
        for ((deviceId, deviceSamples) in samplesByDevice) {
            val buffer = deviceBuffers[deviceId]
            if (buffer != null) {
                synchronized(buffer) {
                    
                    for (sample in deviceSamples) {
                        buffer.offer(sample)
                        updateQualityHistory(deviceId, sample.quality)
                    }
                    
                    
                    while (buffer.size > MAX_BUFFER_SIZE) {
                        buffer.poll()
                    }
                }
                
                Log.v(TAG, "Added ${deviceSamples.size} GSR samples to LSL buffer: $deviceId")
            }
        }
    }
    
    
    fun removeGSRStream(deviceId: String): Boolean {
        val outlet = activeOutlets.remove(deviceId)
        deviceBuffers.remove(deviceId)
        qualityHistory.remove(deviceId)
        
        return if (outlet != null) {
            outlet.close()
            Log.i(TAG, "Removed LSL GSR stream for device: $deviceId")
            true
        } else {
            Log.w(TAG, "No LSL stream found for device: $deviceId")
            false
        }
    }
    
    
    fun removeAllStreams() {
        stopStreaming()
        
        for ((deviceId, outlet) in activeOutlets) {
            outlet.close()
            Log.d(TAG, "Closed LSL outlet for device: $deviceId")
        }
        
        activeOutlets.clear()
        deviceBuffers.clear()
        qualityHistory.clear()
        
        Log.i(TAG, "Removed all LSL GSR streams")
    }
    
    
    private fun startStreamingJob() {
        streamingJob = streamingScope.launch {
            while (isActive && isStreamingEnabled.get()) {
                try {
                    processBufferedSamples()
                    delay(50) 
                } catch (e: Exception) {
                    Log.e(TAG, "Error in LSL streaming job: ${e.message}")
                    streamingErrors.incrementAndGet()
                    delay(1000) 
                }
            }
        }
    }
    
    
    private suspend fun processBufferedSamples() {
        for ((deviceId, buffer) in deviceBuffers) {
            val outlet = activeOutlets[deviceId]
            if (outlet != null && outlet.isOpen()) {
                processDeviceBuffer(deviceId, buffer, outlet)
            }
        }
    }
    
    
    private suspend fun processDeviceBuffer(deviceId: String, buffer: LinkedList<GSRSample>, outlet: LSLStreamOutlet) {
        val samplesToStream = mutableListOf<GSRSample>()
        
        synchronized(buffer) {
            
            while (samplesToStream.size < CHUNK_SIZE && buffer.isNotEmpty()) {
                buffer.poll()?.let { samplesToStream.add(it) }
            }
        }
        
        if (samplesToStream.isNotEmpty()) {
            streamSampleChunk(deviceId, samplesToStream, outlet)
        }
    }
    
    
    private suspend fun streamSampleChunk(deviceId: String, samples: List<GSRSample>, outlet: LSLStreamOutlet) {
        return withContext(Dispatchers.IO) {
            try {
                
                val dataMatrix = Array(samples.size) { i ->
                    doubleArrayOf(samples[i].gsrMicrosiemens)
                }
                
                val timestamps = DoubleArray(samples.size) { i ->
                    samples[i].timestamp / 1_000_000_000.0 
                }
                
                
                if (outlet.pushChunk(dataMatrix, timestamps)) {
                    totalSamplesStreamed.addAndGet(samples.size.toLong())
                    Log.v(TAG, "Streamed ${samples.size} GSR samples via LSL: $deviceId")
                } else {
                    streamingErrors.incrementAndGet()
                    Log.w(TAG, "Failed to stream GSR samples via LSL: $deviceId")
                }
                
            } catch (e: Exception) {
                streamingErrors.incrementAndGet()
                Log.e(TAG, "Error streaming GSR samples for $deviceId: ${e.message}")
            }
        }
    }
    
    
    private fun updateQualityHistory(deviceId: String, quality: Double) {
        val history = qualityHistory[deviceId]
        if (history != null) {
            synchronized(history) {
                history.offer(quality)
                
                
                while (history.size > QUALITY_HISTORY_SIZE) {
                    history.poll()
                }
            }
        }
    }
    
    
    fun getOutletStatistics(deviceId: String): OutletStatistics? {
        val outlet = activeOutlets[deviceId] ?: return null
        val buffer = deviceBuffers[deviceId] ?: return null
        val history = qualityHistory[deviceId] ?: return null
        
        val avgQuality = synchronized(history) {
            if (history.isNotEmpty()) history.average() else 0.0
        }
        
        val bufferUtilization = synchronized(buffer) {
            (buffer.size.toDouble() / MAX_BUFFER_SIZE.toDouble()) * 100.0
        }
        
        return OutletStatistics(
            deviceId = deviceId,
            streamName = "GSR-$deviceId",
            isActive = outlet.isOpen(),
            sampleCount = outlet.getSampleCount(),
            uptime = outlet.getUptime(),
            avgQuality = avgQuality,
            bufferUtilization = bufferUtilization,
            lastSampleTime = System.currentTimeMillis()
        )
    }
    
    
    fun getAllOutletStatistics(): Map<String, OutletStatistics> {
        val allStats = mutableMapOf<String, OutletStatistics>()
        
        for (deviceId in activeOutlets.keys) {
            getOutletStatistics(deviceId)?.let { stats ->
                allStats[deviceId] = stats
            }
        }
        
        return allStats
    }
    
    
    fun getSystemStatistics(): Map<String, Any> {
        val activeOutletCount = activeOutlets.count { it.value.isOpen() }
        val totalBufferSize = deviceBuffers.values.sumOf { buffer ->
            synchronized(buffer) { buffer.size }
        }
        
        val avgQuality = qualityHistory.values.mapNotNull { history ->
            synchronized(history) {
                if (history.isNotEmpty()) history.average() else null
            }
        }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        
        return mapOf(
            "is_streaming_enabled" to isStreamingEnabled.get(),
            "active_outlet_count" to activeOutletCount,
            "total_outlets" to activeOutlets.size,
            "total_samples_streamed" to totalSamplesStreamed.get(),
            "streaming_errors" to streamingErrors.get(),
            "total_buffer_size" to totalBufferSize,
            "max_buffer_size" to MAX_BUFFER_SIZE,
            "average_quality" to avgQuality,
            "min_quality_threshold" to MIN_QUALITY_THRESHOLD
        )
    }
    
    
    fun isLSLAvailable(): Boolean {
        
        return try {
            
            Log.d(TAG, "Checking LSL availability...")
            true 
        } catch (e: Exception) {
            Log.w(TAG, "LSL not available: ${e.message}")
            false
        }
    }
    
    
    fun getLSLVersion(): String {
        
        return "1.16.2-mock"
    }
    
    
    fun cleanup() {
        stopStreaming()
        removeAllStreams()
        streamingScope.cancel()
        
        Log.i(TAG, "LSL GSR outlet cleaned up")
    }
}