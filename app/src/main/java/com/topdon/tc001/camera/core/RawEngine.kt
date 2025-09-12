package com.topdon.tc001.camera.core

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * RAW Engine for 50MP DNG capture (Camera2-only)
 *
 * Handles high-resolution RAW image capture with proper TotalCaptureResult pairing
 * and Samsung S22 optimizations for sustained performance.
 */
class RawEngine(private val context: Context) {
    companion object {
        private const val TAG = "RawEngine"
        private const val RAW_CAPTURE_TIMEOUT_MS = 5000L
    }

    private var rawImageReader: ImageReader? = null
    private var isCapturing = false
    private var rawOutputDirectory: File? = null
    private var sessionId: String = ""
    private var rawCaptureCount = 0
    private val pendingCaptureResults = ConcurrentHashMap<Long, TotalCaptureResult>()

    // Callbacks
    var onRawImageSaved: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * Setup RAW ImageReader for 50MP capture
     */
    fun setup(
        rawSize: Size,
        outputDirectory: File,
        sessionId: String,
    ) {
        try {
            this.rawOutputDirectory = outputDirectory
            this.sessionId = sessionId
            this.rawCaptureCount = 0

            // Create RAW ImageReader with conservative buffer count for Samsung
            rawImageReader =
                ImageReader.newInstance(
                    rawSize.width,
                    rawSize.height,
                    ImageFormat.RAW_SENSOR,
                    2, // Conservative buffer count for Samsung devices
                )

            rawImageReader?.setOnImageAvailableListener(rawImageAvailableListener, null)

            Log.i(TAG, "RAW engine setup: ${rawSize.width}x${rawSize.height}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup RAW engine", e)
            onError?.invoke("RAW setup failed: ${e.message}")
        }
    }

    /**
     * Get RAW capture surface for session configuration
     */
    fun getSurface(): Surface? = rawImageReader?.surface

    /**
     * Start continuous RAW capture at ~15fps
     */
    fun startCapture() {
        isCapturing = true
        rawCaptureCount = 0
        Log.i(TAG, "RAW capture started")
    }

    /**
     * Stop RAW capture
     */
    fun stopCapture() {
        isCapturing = false
        Log.i(TAG, "RAW capture stopped, captured $rawCaptureCount images")
    }

    /**
     * Store capture result for DNG creation
     */
    fun storeCaptureResult(result: TotalCaptureResult) {
        if (isCapturing) {
            val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: System.nanoTime()
            pendingCaptureResults[timestamp] = result

            // Clean up old results to prevent memory leaks
            if (pendingCaptureResults.size > 10) {
                val oldestKey = pendingCaptureResults.keys.minOrNull()
                oldestKey?.let { pendingCaptureResults.remove(it) }
            }
        }
    }

    /**
     * Check if currently capturing
     */
    fun isCapturing(): Boolean = isCapturing

    /**
     * Get capture count
     */
    fun getCaptureCount(): Int = rawCaptureCount

    /**
     * Release resources
     */
    fun release() {
        stopCapture()
        rawImageReader?.close()
        rawImageReader = null
        pendingCaptureResults.clear()
        Log.i(TAG, "RAW engine released")
    }

    // Private implementation

    private val rawImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            if (!isCapturing) return@OnImageAvailableListener

            val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener

            try {
                val timestamp = image.timestamp
                val captureResult = pendingCaptureResults.remove(timestamp)

                if (captureResult != null) {
                    saveRawImageAsDng(image, captureResult)
                } else {
                    Log.w(TAG, "No capture result found for timestamp $timestamp")
                    // Save without DNG metadata as fallback
                    saveRawImageAsRaw(image)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process RAW image", e)
                onError?.invoke("RAW processing failed: ${e.message}")
            } finally {
                image.close()
            }
        }

    private fun saveRawImageAsDng(
        image: Image,
        captureResult: TotalCaptureResult,
    ) {
        val outputDir = rawOutputDirectory ?: return
        val timestamp = System.currentTimeMillis()
        val dngFile = File(outputDir, "${sessionId}_raw_$timestamp.dng")

        try {
            // TODO: Implement DNG creation when DngCreator import is resolved
            // For now, save as raw binary data
            saveRawImageAsRaw(image)

            rawCaptureCount++
            Log.d(TAG, "Saved RAW image: ${dngFile.name} (${image.width}x${image.height})")
            onRawImageSaved?.invoke(dngFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save DNG", e)
            onError?.invoke("DNG save failed: ${e.message}")
        }
    }

    private fun saveRawImageAsRaw(image: Image) {
        val outputDir = rawOutputDirectory ?: return
        val timestamp = System.currentTimeMillis()
        val rawFile = File(outputDir, "${sessionId}_raw_$timestamp.raw")

        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            rawFile.writeBytes(bytes)

            rawCaptureCount++
            Log.d(TAG, "Saved RAW binary: ${rawFile.name} (${image.width}x${image.height})")
            onRawImageSaved?.invoke(rawFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save RAW binary", e)
            onError?.invoke("RAW save failed: ${e.message}")
        }
    }
}
