package com.topdon.tc001.camera.core

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.DngCreator
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * RawEngine wraps RAW ImageReader and DNG creation
 * Handles 50MP RAW_SENSOR capture for Samsung S22
 */
class RawEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "RawEngine"
    }
    
    private var rawImageReader: ImageReader? = null
    private var rawOutputDirectory: File? = null
    private var sessionId: String = ""
    private var rawCaptureCount = 0
    private var isCapturing = false
    
    // For proper DNG creation - pair images with capture results
    private val captureResultMap = ConcurrentHashMap<Long, TotalCaptureResult>()
    
    // Callbacks
    var onRawImageSaved: ((File) -> Unit)? = null
    
    /**
     * Setup RAW ImageReader for 50MP capture
     */
    fun setup(rawSize: Size, outputDirectory: File, sessionId: String) {
        try {
            this.rawOutputDirectory = outputDirectory
            this.sessionId = sessionId
            this.rawCaptureCount = 0
            
            // Create RAW ImageReader with conservative buffer count for Samsung
            rawImageReader = ImageReader.newInstance(
                rawSize.width,
                rawSize.height,
                ImageFormat.RAW_SENSOR,
                2 // Conservative buffer count for Samsung devices
            )
            
            rawImageReader?.setOnImageAvailableListener(rawImageAvailableListener, null)
            
            Log.i(TAG, "RAW engine setup: ${rawSize.width}x${rawSize.height}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup RAW engine", e)
            throw e
        }
    }
    
    /**
     * Get RAW surface for camera session
     */
    fun getSurface(): Surface? = rawImageReader?.surface
    
    /**
     * Start RAW capture
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
        captureResultMap.clear()
        Log.i(TAG, "RAW capture stopped. Captured $rawCaptureCount frames")
    }
    
    /**
     * Store capture result for DNG pairing
     */
    fun storeCaptureResult(result: TotalCaptureResult) {
        val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
        if (timestamp != null) {
            captureResultMap[timestamp] = result
            Log.d(TAG, "Stored capture result for timestamp: $timestamp")
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        stopCapture()
        rawImageReader?.close()
        rawImageReader = null
        Log.d(TAG, "RAW engine released")
    }
    
    /**
     * Get current capture count
     */
    fun getCaptureCount(): Int = rawCaptureCount
    
    /**
     * Check if currently capturing
     */
    fun isCapturing(): Boolean = isCapturing
    
    private val rawImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        if (!isCapturing) return@OnImageAvailableListener
        
        val image = reader.acquireLatestImage()
        if (image != null) {
            try {
                // Find matching capture result
                val imageTimestamp = image.timestamp
                val captureResult = captureResultMap.remove(imageTimestamp)
                
                if (captureResult != null) {
                    saveRawImageAsDng(image, captureResult)
                    rawCaptureCount++
                } else {
                    Log.d(TAG, "No matching capture result for RAW image timestamp: $imageTimestamp")
                }
                
            } finally {
                image.close()
            }
        }
    }
    
    private fun saveRawImageAsDng(image: Image, captureResult: TotalCaptureResult) {
        try {
            val timestamp = System.currentTimeMillis()
            val filename = "RAW_${sessionId}_${String.format("%06d", rawCaptureCount)}_$timestamp.dng"
            val dngFile = File(rawOutputDirectory, filename)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Get camera characteristics for DNG creation
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = "0" // Use main camera ID - could be parameterized
                val characteristics = manager.getCameraCharacteristics(cameraId)
                
                FileOutputStream(dngFile).use { output ->
                    val dngCreator = DngCreator(characteristics, captureResult)
                    dngCreator.writeImage(output, image)
                    dngCreator.close()
                }
                
                Log.d(TAG, "Saved RAW DNG: ${dngFile.name} (${image.width}x${image.height})")
                onRawImageSaved?.invoke(dngFile)
                
            } else {
                Log.w(TAG, "DNG creation not supported on API level ${Build.VERSION.SDK_INT}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save RAW image as DNG", e)
        }
    }
}