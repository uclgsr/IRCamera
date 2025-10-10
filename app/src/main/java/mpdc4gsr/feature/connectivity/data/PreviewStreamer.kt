package mpdc4gsr.feature.connectivity.data

import android.graphics.Bitmap
import android.util.Base64
import com.mpdc4gsr.libunified.app.utils.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.AppLogger
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PreviewStreamer(
    private val networkServer: NetworkServer,
) {
    companion object {
        private const val DEFAULT_FRAME_INTERVAL_MS = 1_000L
        private const val DEFAULT_SENSOR_INTERVAL_MS = 1_000L
        private const val DEFAULT_PREVIEW_WIDTH = 320
        private const val DEFAULT_PREVIEW_HEIGHT = 240
        private const val DEFAULT_JPEG_QUALITY = 70
    }


    private val isStreaming = AtomicBoolean(false)
    private val streamingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
    private val currentRecordingStatus = AtomicReference("IDLE")

    suspend fun startStreaming(): Boolean =
        withContext(Dispatchers.IO) {
            if (isStreaming.get()) {
                return@withContext true
            }

            if (!networkServer.isClientConnected()) {
                return@withContext false
            }

            isStreaming.set(true)
            frameStreamingJob = streamingScope.launch { streamFrames() }

            sensorStreamingJob = streamingScope.launch { streamSensorData() }

            true
        }


    suspend fun stopStreaming() {
        withContext(Dispatchers.IO) {
            if (!isStreaming.compareAndSet(true, false)) {
                return@withContext
            }

            frameStreamingJob?.cancel()
            sensorStreamingJob?.cancel()
            frameStreamingJob = null
            sensorStreamingJob = null
        }
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
        jpegQuality: Int = DEFAULT_JPEG_QUALITY,
    ) {
        this.frameIntervalMs = frameIntervalMs
        this.sensorIntervalMs = sensorIntervalMs
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        this.jpegQuality = jpegQuality
    }


    private suspend fun streamFrames() {
        while (isStreaming.get() && streamingScope.isActive) {
            try {
                currentRgbFrame.get()?.let { frame ->
                    streamFrame("rgb", frame)
                }

                currentThermalFrame.get()?.let { frame ->
                    streamFrame("thermal", frame)
                }

                delay(frameIntervalMs)
            } catch (e: Exception) {
                if (isStreaming.get()) {
                    AppLogger.e("PreviewStreamer", "Frame streaming error", e)
                    delay(1_000)
                } else {
                    break
                }
            }
        }
    }


    private suspend fun streamSensorData() {
        while (isStreaming.get() && streamingScope.isActive) {
            try {
                val sensorPayload =
                    JSONObject()
                        .put("message_type", "sensor_data")
                        .put("timestamp_ns", System.nanoTime())
                        .put(
                            "data",
                            JSONObject().apply {
                                currentGsrValue.get()?.let { put("gsr_microsiemens", it) }

                                put("recording_status", currentRecordingStatus.get())
                                put("client_count", if (networkServer.isClientConnected()) 1 else 0)
                            },
                        )
                networkServer.sendMessage(sensorPayload.toString())
                delay(sensorIntervalMs)
            } catch (e: Exception) {
                if (isStreaming.get()) {
                    AppLogger.e("PreviewStreamer", "Sensor streaming error", e)
                    delay(1_000)
                } else {
                    break
                }
            }
        }
    }


    private suspend fun streamFrame(frameType: String, sourceBitmap: Bitmap) {
        val scaledBitmap =
            BitmapUtils.scaleWithWH(
                sourceBitmap,
                previewWidth.toDouble(),
                previewHeight.toDouble(),
            )
        val jpegBytes =
            BitmapUtils.bitmapToBytes(scaledBitmap, jpegQuality)
                ?: return

        val frameMessage =
            JSONObject()
                .put("message_type", "preview_frame")
                .put("timestamp_ns", System.nanoTime())
                .put("frame_type", frameType)
                .put("width", scaledBitmap.width)
                .put("height", scaledBitmap.height)
                .put("format", "jpeg")
                .put("quality", jpegQuality)
                .put("data_base64", Base64.encodeToString(jpegBytes, Base64.NO_WRAP))
                .put("data_size_bytes", jpegBytes.size)

        val success = networkServer.sendMessage(frameMessage.toString())
        if (!success) {
            AppLogger.w("PreviewStreamer", "Failed to send preview frame")
        }


        if (scaledBitmap !== sourceBitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }
    }


    fun isStreaming(): Boolean = isStreaming.get()

    fun cleanup() {
        streamingScope.launch {
            stopStreaming()
            streamingScope.coroutineContext[Job]?.cancel()
        }
    }
}
