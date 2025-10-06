package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.elvishew.xlog.XLog

class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val TAG = "CameraPreView"
    private var cameraAlpha: Float = 1.0f
    private var isRotationSet: Boolean = false

    interface CameraPreViewCloseListener {
        fun onClose()
    }

    private var closeListener: CameraPreViewCloseListener? = null
    var cameraPreViewCloseListener: CameraPreViewCloseListener? = null
        set(value) {
            field = value
            closeListener = value
        }

    override fun setRotation(rotation: Float) {
        super.setRotation(rotation)
        isRotationSet = true
        XLog.d(TAG, "Camera rotation set to: $rotation")
    }

    fun setRotation(enabled: Boolean) {
        isRotationSet = enabled
        if (!enabled) {
            super.setRotation(0f)
        }
        XLog.d(TAG, "Camera rotation enabled: $enabled")
    }

    fun setCameraAlpha(alpha: Float) {
        this.cameraAlpha = alpha
        this.alpha = alpha
        XLog.d(TAG, "Camera alpha set to: $alpha")
    }

    fun getCameraAlpha(): Float = cameraAlpha
    fun openCamera() {
        visibility = VISIBLE
        XLog.d(TAG, "Camera opened")
    }

    fun closeCamera() {
        visibility = GONE
        closeListener?.onClose()
        XLog.d(TAG, "Camera closed")
    }

    fun getBitmap(): Bitmap? {
        // This would typically capture the current frame
        // For now, return null as placeholder
        XLog.d(TAG, "Bitmap requested")
        return null
    }

    fun getBitmap(width: Int, height: Int): Bitmap? {
        // This would typically capture and scale the current frame
        XLog.d(TAG, "Scaled bitmap requested: ${width}x${height}")
        return null
    }

    fun setOnCloseListener(listener: CameraPreViewCloseListener) {
        this.closeListener = listener
    }

    override fun post(action: Runnable): Boolean {
        return super.post(action)
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        XLog.d(TAG, "Layout params updated")
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        when (visibility) {
            VISIBLE -> XLog.d(TAG, "Camera preview visible")
            GONE -> XLog.d(TAG, "Camera preview gone")
            INVISIBLE -> XLog.d(TAG, "Camera preview invisible")
        }
    }
}