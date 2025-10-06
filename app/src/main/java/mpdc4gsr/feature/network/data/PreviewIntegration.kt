package mpdc4gsr.feature.network.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import mpdc4gsr.core.RecordingService

object PreviewIntegration {
    private const val TAG = "PreviewIntegration"
    fun updateRgbFrame(context: Context, rgbFrame: Bitmap) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateRgbFrame(rgbFrame)
            AppLogger.v(TAG, "Updated RGB frame for preview: ${rgbFrame.width}x${rgbFrame.height}")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to update RGB frame for preview", e)
        }
    }

    fun updateThermalFrame(context: Context, thermalFrame: Bitmap) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateThermalFrameDirect(thermalFrame)
            Log.v(
                TAG,
                "Updated thermal frame for preview: ${thermalFrame.width}x${thermalFrame.height}"
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to update thermal frame for preview", e)
        }
    }

    fun updateGsrValue(context: Context, gsrValue: Float) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateGsrValueDirect(gsrValue)
            AppLogger.v(TAG, "Updated GSR value for preview: ${gsrValue}µS")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to update GSR value for preview", e)
        }
    }

    fun isPreviewStreamingActive(context: Context): Boolean {
        return try {
            val streamer = getPreviewStreamer(context)
            streamer?.isStreaming() == true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to check preview streaming status", e)
            false
        }
    }

    fun getStreamingConfig(context: Context): Map<String, Any> {
        return try {
            emptyMap()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get streaming configuration", e)
            emptyMap()
        }
    }

    fun configureStreaming(
        context: Context,
        frameIntervalMs: Long = 1000L,
        sensorIntervalMs: Long = 1000L,
        previewWidth: Int = 320,
        previewHeight: Int = 240,
        jpegQuality: Int = 70
    ) {
        try {
            val streamer = getPreviewStreamer(context)
            streamer?.configure(
                frameIntervalMs,
                sensorIntervalMs,
                previewWidth,
                previewHeight,
                jpegQuality
            )
            Log.i(
                TAG,
                "Configured preview streaming: ${frameIntervalMs}ms frames, ${previewWidth}x${previewHeight}@${jpegQuality}%"
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to configure preview streaming", e)
        }
    }

    private fun getPreviewDataAdapter(context: Context): PreviewDataAdapter? {
        val service = getRecordingService(context)
        return service?.previewDataAdapter
    }

    private fun getPreviewStreamer(context: Context): PreviewStreamer? {
        val service = getRecordingService(context)
        return service?.previewStreamer
    }

    private fun getRecordingService(context: Context): RecordingService? {
        AppLogger.d(TAG, "Note: RecordingService access needs proper implementation via service binding")
        return null
    }
}

fun com.mpdc4gsr.module.thermalunified.tools.CameraPreviewManager.updatePreview(context: Context) {
    try {
        val bitmap = this.scaledBitmap()
        if (bitmap != null && !bitmap.isRecycled) {
            PreviewIntegration.updateThermalFrame(context, bitmap)
        }
    } catch (e: Exception) {
        AppLogger.w("PreviewIntegration", "Failed to update thermal preview from CameraPreviewManager", e)
    }
}

fun Float.updateGsrPreview(context: Context) {
    PreviewIntegration.updateGsrValue(context, this)
}
