package com.mpdc4gsr.libunified.ui.camera

import android.graphics.Bitmap
import android.os.Handler
import com.elvishew.xlog.XLog
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.common.RotateDegree

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

    // Property that accepts RotateDegree enum
    var imageRotateDegree: RotateDegree = RotateDegree.DEGREE_0
        set(value) {
            field = value
            imageRotate = value.getValue()
            XLog.d(TAG, "Image rotate degree set to: $value")
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

    // Frame data for thermal imaging
    var frameIrAndTempData: ByteArray? = null
        set(value) {
            field = value
            XLog.d(TAG, "Frame IR and temp data updated")
        }

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

    fun updateCameraBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            cameraView?.bitmap = it
            scaledBitmapCache = null // Invalidate cache
        }
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

    fun releaseSource() {
        release()
        XLog.d(TAG, "CameraPreviewManager source released")
    }

    // Method overloads for different parameter combinations
    fun setLimit(min: Float, max: Float, param3: Any) {
        setLimit(min, max)
        XLog.d(TAG, "Temperature limits set with additional parameter: min=$min, max=$max")
    }

    fun setColorList(colors: Nothing?, nothing: Nothing?, bool: Boolean, f: Float, f1: Float) {
        XLog.d(TAG, "Color list set")
    }

    fun setAutoSwitchGainEnable(enabled: Boolean) {
        XLog.d(TAG, "Auto switch gain enabled: $enabled")
    }

    // Camera view initialization with different types
    fun init(surfaceView: Any, handler: Handler) {
        this.handler = handler
        XLog.d(TAG, "CameraPreviewManager initialized with surface view")
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