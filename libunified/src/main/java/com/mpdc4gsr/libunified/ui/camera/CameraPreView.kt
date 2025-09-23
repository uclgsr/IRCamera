package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import androidx.core.view.drawToBitmap
import com.mpdc4gsr.libunified.app.listener.BitmapViewListener

class CameraPreView : View, BitmapViewListener {
    var cameraPreViewCloseListener: (() -> Unit)? = null
    
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    fun getBitmap(): Bitmap? {
        return try {
            drawToBitmap()
        } catch (e: Exception) {
            null
        }
    }
    
    fun setCameraAlpha(alpha: Float) {
        this.alpha = alpha
    }
    
    fun closeCamera() {
        // Placeholder implementation
    }
    
    fun openCamera() {
        // Placeholder implementation
    }
    
    override fun setRotation(rotation: Float) {
        super.setRotation(rotation)
    }
    
    fun setRotation(rotation: Boolean) {
        // Convert boolean to rotation angle
        setRotation(if (rotation) 180f else 0f)
    }
    
    // BitmapViewListener implementation
    override val viewX: Float get() = x
    override val viewY: Float get() = y
    override val viewAlpha: Float get() = alpha
    override val viewWidth: Float get() = width.toFloat()
    override val viewHeight: Float get() = height.toFloat()
    override val viewScale: Float get() = scaleX // or scaleY, assuming uniform scaling
}