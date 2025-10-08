package mpdc4gsr.feature.network.data

import android.content.Context
import android.graphics.Bitmap
import mpdc4gsr.feature.system.service.RecordingService

object PreviewIntegration {
    fun updateRgbFrame(context: Context, rgbFrame: Bitmap) {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateRgbFrame(rgbFrame)
        }
    }

    fun updateThermalFrame(context: Context, thermalFrame: Bitmap) {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateThermalFrameDirect(thermalFrame)
                TAG,
                "Updated thermal frame for preview: ${thermalFrame.width}x${thermalFrame.height}"
            )
        }
    }

    fun updateGsrValue(context: Context, gsrValue: Float) {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateGsrValueDirect(gsrValue)
        }
    }

    fun isPreviewStreamingActive(context: Context): Boolean {
        return (
            val streamer = getPreviewStreamer(context)
            streamer?.isStreaming() == true
            false
        }
    }

    fun getStreamingConfig(context: Context): Map<String, Any> {
        return (
            emptyMap()
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
            val streamer = getPreviewStreamer(context)
            streamer?.configure(
                frameIntervalMs,
                sensorIntervalMs,
                previewWidth,
                previewHeight,
                jpegQuality
            )
                TAG,
                "Configured preview streaming: ${frameIntervalMs}ms frames, ${previewWidth}x${previewHeight}@${jpegQuality}%"
            )
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
        val bitmap = this.scaledBitmap()
        if (bitmap != null && !bitmap.isRecycled) {
            PreviewIntegration.updateThermalFrame(context, bitmap)
        }
    }
}

fun Float.updateGsrPreview(context: Context) {
    PreviewIntegration.updateGsrValue(context, this)
}
