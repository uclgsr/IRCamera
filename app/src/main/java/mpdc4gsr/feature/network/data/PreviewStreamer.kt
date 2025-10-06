package mpdc4gsr.feature.network.data
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import com.mpdc4gsr.libunified.app.utils.BitmapUtils
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
class PreviewStreamer(
    private val networkServer: NetworkServer
) {
    companion object {
        private const val TAG = "PreviewStreamer"
        private const val DEFAULT_FRAME_INTERVAL_MS = 1000L
        private const val DEFAULT_SENSOR_INTERVAL_MS = 1000L
        private const val DEFAULT_PREVIEW_WIDTH = 320
        private const val DEFAULT_PREVIEW_HEIGHT = 240
        private const val DEFAULT_JPEG_QUALITY = 70
    }
    private val isStreaming = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var frameStreamingJob: Job? = null
    private var sensorStreamingJob: Job? = null
    private var frameIntervalMs = DEFAULT_FRAME_INTERVAL_MS
    private var sensorIntervalMs = DEFAULT_SENSOR_INTERVAL_MS
    private var previewWidth = DEFAULT_PREVIEW_WIDTH
    private var previewHeight = DEFAULT_PREVIEW_HEIGHT
    private var jpegQuality = DEFAULT_JPEG_QUALITY
    private val currentRgbFrame = AtomicReference<Bitmap?>()
    private val currentThermalFrame = AtomicReference<Bitmap?>()
    private val currentGsrValue = AtomicReference<Float?>()
    private val currentRecordingStatus = AtomicReference<String>("IDLE")
    suspend fun startStreaming(): Boolean {
        if (isStreaming.get()) {
            AppLogger.w(TAG, "Preview streaming already active")
            return true
        }
        if (!networkServer.isClientConnected()) {
            AppLogger.w(TAG, "No PC client connected, cannot start streaming")
            return false
        }
        AppLogger.i(TAG, "Starting preview streaming to PC")
        isStreaming.set(true)
        frameStreamingJob = scope.launch {
            streamFrames()
        }
        sensorStreamingJob = scope.launch {
            streamSensorData()
        }
        return true
    }
    suspend fun stopStreaming() {
        if (!isStreaming.get()) {
            return
        }
        AppLogger.i(TAG, "Stopping preview streaming")
        isStreaming.set(false)
        frameStreamingJob?.cancel()
        sensorStreamingJob?.cancel()
        frameStreamingJob = null
        sensorStreamingJob = null
    }
    fun updateRgbFrame(bitmap: Bitmap?) {
        currentRgbFrame.set(bitmap)
    }
    fun updateThermalFrame(bitmap: Bitmap?) {
        currentThermalFrame.set(bitmap)
    }
    fun updateGsrValue(gsrValue: Float) {
        currentGsrValue.set(gsrValue)
    }
    fun updateRecordingStatus(status: String) {
        currentRecordingStatus.set(status)
    }
    fun configure(
        frameIntervalMs: Long = DEFAULT_FRAME_INTERVAL_MS,
        sensorIntervalMs: Long = DEFAULT_SENSOR_INTERVAL_MS,
        previewWidth: Int = DEFAULT_PREVIEW_WIDTH,
        previewHeight: Int = DEFAULT_PREVIEW_HEIGHT,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY
    ) {
        this.frameIntervalMs = frameIntervalMs
        this.sensorIntervalMs = sensorIntervalMs
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        this.jpegQuality = jpegQuality
        Log.i(
            TAG,
            "Preview streaming configured: ${frameIntervalMs}ms frames, ${sensorIntervalMs}ms sensors, ${previewWidth}x${previewHeight}@${jpegQuality}%"
        )
    }
    private suspend fun streamFrames() {
        AppLogger.i(TAG, "Frame streaming started")
        while (currentCoroutineContext().isActive && isStreaming.get()) {
            try {
                currentRgbFrame.get()?.let { rgbBitmap ->
                    streamFrame("rgb", rgbBitmap)
                }
                currentThermalFrame.get()?.let { thermalBitmap ->
                    streamFrame("thermal", thermalBitmap)
                }
                delay(frameIntervalMs)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error in frame streaming", e)
                if (currentCoroutineContext().isActive) delay(1000)
            }
        }
        AppLogger.i(TAG, "Frame streaming stopped")
    }
    private suspend fun streamSensorData() {
        AppLogger.i(TAG, "Sensor data streaming started")
        while (currentCoroutineContext().isActive && isStreaming.get()) {
            try {
                val gsrValue = currentGsrValue.get()
                val recordingStatus = currentRecordingStatus.get()
                val sensorMessage = JSONObject().apply {
                    put("message_type", "sensor_data")
                    put("timestamp_ns", System.nanoTime())
                    put("data", JSONObject().apply {
                        if (gsrValue != null) {
                            put("gsr_microsiemens", gsrValue)
                        }
                        put("recording_status", recordingStatus)
                        put("client_count", if (networkServer.isClientConnected()) 1 else 0)
                    })
                }
                networkServer.sendMessage(sensorMessage.toString())
                delay(sensorIntervalMs)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error in sensor data streaming", e)
                if (currentCoroutineContext().isActive) delay(1000)
            }
        }
        AppLogger.i(TAG, "Sensor data streaming stopped")
    }
    private suspend fun streamFrame(frameType: String, bitmap: Bitmap) {
        try {
            val scaledBitmap = BitmapUtils.scaleWithWH(
                bitmap,
                previewWidth.toDouble(),
                previewHeight.toDouble()
            )
            val jpegBytes = BitmapUtils.bitmapToBytes(scaledBitmap, jpegQuality)
            if (jpegBytes == null) {
                AppLogger.w(TAG, "Failed to convert $frameType frame to JPEG")
                return
            }
            val base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val frameMessage = JSONObject().apply {
                put("message_type", "preview_frame")
                put("timestamp_ns", System.nanoTime())
                put("frame_type", frameType)
                put("width", scaledBitmap.width)
                put("height", scaledBitmap.height)
                put("format", "jpeg")
                put("quality", jpegQuality)
                put("data_base64", base64Data)
                put("data_size_bytes", jpegBytes.size)
            }
            networkServer.sendMessage(frameMessage.toString())
            Log.d(
                TAG,
                "Streamed $frameType frame: ${scaledBitmap.width}x${scaledBitmap.height}, ${jpegBytes.size} bytes"
            )
            if (scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error streaming $frameType frame", e)
        }
    }
    fun isStreaming(): Boolean = isStreaming.get()
    fun cleanup() {
        scope.launch {
            stopStreaming()
        }
        scope.coroutineContext.job.cancel()
        AppLogger.i(TAG, "PreviewStreamer cleaned up")
    }
}
