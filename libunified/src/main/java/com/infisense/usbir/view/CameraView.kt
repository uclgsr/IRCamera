package com.infisense.usbir.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View

/**
 * Stub implementation for CameraView to enable compilation
 * This is a minimal implementation for MVP - replace with actual implementation when available
 */
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var bitmap: Bitmap? = null
    var isOpenAmplify: Boolean = false
    var productType: Int = 0
    
    fun setTextSize(size: Float) {
        // Stub implementation
    }
    
    fun setLinePaintColor(color: Int) {
        // Stub implementation
    }
    
    fun setMirror(mirror: Boolean) {
        // Stub implementation
    }
    
    fun stop() {
        // Stub implementation
    }
}