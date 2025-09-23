package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import androidx.core.view.drawToBitmap

class CameraPreView : View {
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
    
    fun setRotation(rotation: Boolean) {
        // Convert boolean to rotation angle
        this.rotation = if (rotation) 180f else 0f
    }
    
    fun setRotation(rotation: Float) {
        this.rotation = rotation
    }
}