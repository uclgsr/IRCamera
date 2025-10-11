package com.mpdc4gsr.component.shared.ui.camera

import android.graphics.Bitmap
import android.os.Handler
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.component.shared.app.bean.AlarmBean
import com.mpdc4gsr.component.shared.common.RotateDegree

class CameraPreviewManager private constructor() {
    private val TAG = "CameraPreviewManager"

    companion object {
        @Volatile
        private var INSTANCE: CameraPreviewManager? = null

        fun getInstance(): CameraPreviewManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraPreviewManager().also { INSTANCE = it }
            }
    }

    // Properties
    var imageRotate: Int = 0
        set(value) {
            field = value
        }

    // Property that accepts RotateDegree enum
    var imageRotateDegree: RotateDegree = RotateDegree.DEGREE_0
        set(value) {
            field = value
            imageRotate = value.getValue()
        }
    var alarmBean: AlarmBean? = null
        set(value) {
            field = value
        }
    private var cameraView: CameraView? = null
    private var handler: Handler? = null
    private var tempDataChangeCallback: ((Any) -> Unit)? = null
    private var scaledBitmapCache: Bitmap? = null

    // Frame data for thermal imaging
    var frameIrAndTempData: ByteArray? = null
        set(value) {
            field = value
        }

    // Configuration
    private var minLimit: Float = 0f
    private var maxLimit: Float = 100f
    private var pseudocolorMode: Int = 0

    fun init(
        cameraView: CameraView,
        handler: Handler,
    ) {
        this.cameraView = cameraView
        this.handler = handler
    }

    fun setLimit(
        min: Float,
        max: Float,
    ) {
        this.minLimit = min
        this.maxLimit = max
    }

    fun setPseudocolorMode(mode: Int) {
        this.pseudocolorMode = mode
    }

    fun setOnTempDataChangeCallback(callback: (Any) -> Unit) {
        this.tempDataChangeCallback = callback
    }

    fun scaledBitmap(cached: Boolean = false): Bitmap? =
        if (cached && scaledBitmapCache != null) {
            scaledBitmapCache
        } else {
            cameraView?.getScaledBitmap()?.also { bitmap ->
                if (cached) {
                    scaledBitmapCache = bitmap
                }
            }
        }

    fun getCameraBitmap(): Bitmap? = cameraView?.bitmap

    fun updateCameraBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            cameraView?.bitmap = it
            scaledBitmapCache = null // Invalidate cache
        }
    }

    fun startPreview() {
        cameraView?.start()
    }

    fun stopPreview() {
        cameraView?.stop()
        scaledBitmapCache = null
    }

    fun pausePreview() {
    }

    fun resumePreview() {
    }

    fun release() {
        cameraView = null
        handler = null
        tempDataChangeCallback = null
        scaledBitmapCache = null
    }

    fun releaseSource() {
        release()
    }

    // Method overloads for different parameter combinations
    fun setLimit(
        min: Float,
        max: Float,
        param3: Any,
    ) {
        setLimit(min, max)
    }

    fun setColorList(
        colors: Nothing?,
        nothing: Nothing?,
        bool: Boolean,
        f: Float,
        f1: Float,
    ) {
    }

    fun setAutoSwitchGainEnable(enabled: Boolean) {
    }

    // Camera view initialization with different types
    fun init(
        surfaceView: Any,
        handler: Handler,
    ) {
        this.handler = handler
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

    fun setTemperatureLimits(
        min: Float,
        max: Float,
    ) {
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


