package mpdc4gsr.feature.network.data

import android.content.Context
import android.graphics.Bitmap
import mpdc4gsr.core.RecordingService

object PreviewIntegration {
    fun updateRgbFrame(context: Context, rgbFrame: Bitmap) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateRgbFrame(rgbFrame)
        } catch (e: Exception) {
        }
    }

    fun updateThermalFrame(context: Context, thermalFrame: Bitmap) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateThermalFrameDirect(thermalFrame)
        } catch (e: Exception) {
        }
    }

    fun updateGsrValue(context: Context, gsrValue: Float) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateGsrValueDirect(gsrValue)
        } catch (e: Exception) {
        }
    }

    fun isPreviewStreamingActive(context: Context): Boolean {
        return try {
            val streamer = getPreviewStreamer(context)
            streamer?.isStreaming() == true
        } catch (e: Exception) {
            false
        }
    }

    fun getStreamingConfig(context: Context): Map<String, Any> {
        return try {
            emptyMap()
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
    }
}

fun Float.updateGsrPreview(context: Context) {
    PreviewIntegration.updateGsrValue(context, this)
}
