package mpdc4gsr.network

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import mpdc4gsr.service.RecordingService

/**
 * PreviewIntegration provides easy integration points for camera and sensor components
 * to feed data into the live preview streaming system.
 *
 * This is a utility class that other components can use to update preview data
 * without needing to directly manage the PreviewStreamer or PreviewDataAdapter.
 */
object PreviewIntegration {
    private const val TAG = "PreviewIntegration"

    /**
     * Update RGB camera frame for live preview streaming.
     * Call this whenever a new RGB camera frame is available.
     *
     * @param context Application context to access RecordingService
     * @param rgbFrame The RGB camera frame bitmap
     */
    fun updateRgbFrame(context: Context, rgbFrame: Bitmap) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateRgbFrame(rgbFrame)
            Log.v(TAG, "Updated RGB frame for preview: ${rgbFrame.width}x${rgbFrame.height}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update RGB frame for preview", e)
        }
    }

    /**
     * Update thermal camera frame for live preview streaming.
     * Call this whenever a new thermal camera frame is available.
     *
     * @param context Application context to access RecordingService
     * @param thermalFrame The thermal camera frame bitmap
     */
    fun updateThermalFrame(context: Context, thermalFrame: Bitmap) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateThermalFrameDirect(thermalFrame)
            Log.v(
                TAG,
                "Updated thermal frame for preview: ${thermalFrame.width}x${thermalFrame.height}"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update thermal frame for preview", e)
        }
    }

    /**
     * Update GSR sensor value for live preview streaming.
     * Call this whenever a new GSR reading is available.
     *
     * @param context Application context to access RecordingService
     * @param gsrValue The GSR value in microsiemens
     */
    fun updateGsrValue(context: Context, gsrValue: Float) {
        try {
            val adapter = getPreviewDataAdapter(context)
            adapter?.updateGsrValueDirect(gsrValue)
            Log.v(TAG, "Updated GSR value for preview: ${gsrValue}µS")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update GSR value for preview", e)
        }
    }

    /**
     * Check if preview streaming is currently active.
     *
     * @param context Application context to access RecordingService
     * @return true if preview streaming is active, false otherwise
     */
    fun isPreviewStreamingActive(context: Context): Boolean {
        return try {
            val streamer = getPreviewStreamer(context)
            streamer?.isStreaming() == true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check preview streaming status", e)
            false
        }
    }

    /**
     * Get the current preview streaming configuration.
     *
     * @param context Application context to access RecordingService
     * @return Map of configuration parameters, or empty map if not available
     */
    fun getStreamingConfig(context: Context): Map<String, Any> {
        return try {
            // This would require extending PreviewStreamer to expose config
            // For now, return empty map
            emptyMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get streaming configuration", e)
            emptyMap()
        }
    }

    /**
     * Configure preview streaming parameters.
     *
     * @param context Application context to access RecordingService
     * @param frameIntervalMs Interval between frame updates in milliseconds
     * @param sensorIntervalMs Interval between sensor updates in milliseconds
     * @param previewWidth Preview frame width in pixels
     * @param previewHeight Preview frame height in pixels
     * @param jpegQuality JPEG compression quality (1-100)
     */
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
            Log.w(TAG, "Failed to configure preview streaming", e)
        }
    }

    // Helper methods to access service components

    private fun getPreviewDataAdapter(context: Context): PreviewDataAdapter? {
        val service = getRecordingService(context)
        return service?.previewDataAdapter
    }

    private fun getPreviewStreamer(context: Context): PreviewStreamer? {
        val service = getRecordingService(context)
        return service?.previewStreamer
    }

    private fun getRecordingService(context: Context): RecordingService? {
        // This is a simplified approach - in a real implementation you might:
        // 1. Use a service connection/binding
        // 2. Access through a singleton pattern
        // 3. Use dependency injection
        // 
        // For now, we'll log that this needs proper service access
        Log.d(TAG, "Note: RecordingService access needs proper implementation via service binding")
        return null
    }
}

/**
 * Extension functions for easier integration from specific components
 */

/**
 * Extension function for thermal camera components to easily update preview
 */
fun com.example.thermal_lite.camera.CameraPreviewManager.updatePreview(context: Context) {
    try {
        val bitmap = this.scaledBitmap()
        if (bitmap != null && !bitmap.isRecycled) {
            PreviewIntegration.updateThermalFrame(context, bitmap)
        }
    } catch (e: Exception) {
        Log.w("PreviewIntegration", "Failed to update thermal preview from CameraPreviewManager", e)
    }
}

/**
 * Extension function for GSR sensor components to easily update preview
 */
fun Float.updateGsrPreview(context: Context) {
    PreviewIntegration.updateGsrValue(context, this)
}
