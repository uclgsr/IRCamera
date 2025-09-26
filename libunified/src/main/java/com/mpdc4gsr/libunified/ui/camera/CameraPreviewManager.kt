package com.mpdc4gsr.libunified.ui.camera

import android.graphics.Bitmap
import android.os.Handler
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.bean.AlarmBean

/**
 * Camera preview manager for thermal camera operations
 * Based on IRCamera groundtruth implementation patterns
 */
class CameraPreviewManager private constructor() {
    
    private val TAG = "CameraPreviewManager"
    
    companion object {
        @Volatile
        private var INSTANCE: CameraPreviewManager? = null
        
        fun getInstance(): CameraPreviewManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraPreviewManager().also { INSTANCE = it }
            }
        }
    }
    
    // Properties
    var imageRotate: Int = 0
        set(value) {
            field = value
            XLog.d(TAG, "Image rotate set to: $value")
        }
    
    var alarmBean: AlarmBean? = null
        set(value) {
            field = value
            XLog.d(TAG, "Alarm bean updated")
        }
    
    private var cameraView: CameraView? = null
    private var handler: Handler? = null
    private var tempDataChangeCallback: ((Any) -> Unit)? = null
    private var scaledBitmapCache: Bitmap? = null
    
    // Configuration
    private var minLimit: Float = 0f
    private var maxLimit: Float = 100f
    private var pseudocolorMode: Int = 0
    
    fun init(cameraView: CameraView, handler: Handler) {
        this.cameraView = cameraView
        this.handler = handler
        XLog.d(TAG, "CameraPreviewManager initialized")
    }
    
    fun setLimit(min: Float, max: Float) {
        this.minLimit = min
        this.maxLimit = max
        XLog.d(TAG, "Temperature limits set: min=$min, max=$max")
    }
    
    fun setPseudocolorMode(mode: Int) {
        this.pseudocolorMode = mode
        XLog.d(TAG, "Pseudocolor mode set to: $mode")
    }
    
    fun setOnTempDataChangeCallback(callback: (Any) -> Unit) {
        this.tempDataChangeCallback = callback
        XLog.d(TAG, "Temperature data change callback set")
    }
    
    fun scaledBitmap(cached: Boolean = false): Bitmap? {
        return if (cached && scaledBitmapCache != null) {
            scaledBitmapCache
        } else {
            cameraView?.getScaledBitmap()?.also { bitmap ->
                if (cached) {
                    scaledBitmapCache = bitmap
                }
            }
        }
    }
    
    fun getCameraBitmap(): Bitmap? {
        return cameraView?.bitmap
    }
    
    fun updateCameraBitmap(bitmap: Bitmap) {
        cameraView?.bitmap = bitmap
        scaledBitmapCache = null // Invalidate cache
    }
    
    fun startPreview() {
        cameraView?.start()
        XLog.d(TAG, "Preview started")
    }
    
    fun stopPreview() {
        cameraView?.stop()
        scaledBitmapCache = null
        XLog.d(TAG, "Preview stopped")
    }
    
    fun pausePreview() {
        XLog.d(TAG, "Preview paused")
    }
    
    fun resumePreview() {
        XLog.d(TAG, "Preview resumed")
    }
    
    fun release() {
        cameraView = null
        handler = null
        tempDataChangeCallback = null
        scaledBitmapCache = null
        XLog.d(TAG, "CameraPreviewManager released")
    }
    
    // Thermal data processing
    fun processThermalData(data: Any) {
        tempDataChangeCallback?.invoke(data)
    }
    
    // Camera controls
    fun setImageRotation(rotation: Int) {
        imageRotate = rotation
    }
    
    fun getImageRotation(): Int = imageRotate
    
    fun setTemperatureLimits(min: Float, max: Float) {
        setLimit(min, max)
    }
    
    fun getMinTemperature(): Float = minLimit
    fun getMaxTemperature(): Float = maxLimit
    
    // Pseudocolor controls
    fun getPseudocolorMode(): Int = pseudocolorMode
    
    fun applyPseudocolor(bitmap: Bitmap): Bitmap {
        // Placeholder for pseudocolor processing
        // In real implementation, this would apply thermal color mapping
        return bitmap
    }
}