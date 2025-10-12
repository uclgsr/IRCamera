package com.mpdc4gsr.component.shared.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

class CameraPreView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
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
    }

    fun setRotation(enabled: Boolean) {
        isRotationSet = enabled
        if (!enabled) {
            super.setRotation(0f)
        }
    }

    fun setCameraAlpha(alpha: Float) {
        this.cameraAlpha = alpha
        this.alpha = alpha
    }

    fun getCameraAlpha(): Float = cameraAlpha

    fun openCamera() {
        visibility = VISIBLE
    }

    fun closeCamera() {
        visibility = GONE
        closeListener?.onClose()
    }

    fun getBitmap(): Bitmap? {
        // This would typically capture the current frame
        // For now, return null as placeholder
        return null
    }

    fun getBitmap(
        width: Int,
        height: Int,
    ): Bitmap? {
        // This would typically capture and scale the current frame
        return null
    }

    fun setOnCloseListener(listener: CameraPreViewCloseListener) {
        this.closeListener = listener
    }

    override fun post(action: Runnable): Boolean = super.post(action)

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        when (visibility) {
            VISIBLE -> Unit
            GONE -> Unit
            INVISIBLE -> Unit
        }
    }
}


