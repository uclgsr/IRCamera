package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Stub implementation for CameraPreView to enable compilation
 * This is a minimal implementation for MVP - replace with actual camera view when available
 */
class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    fun closeCamera() {
        // Stub implementation - does nothing for MVP
    }
    
    fun openCamera() {
        // Stub implementation - does nothing for MVP
    }
    
    fun getBitmap(): android.graphics.Bitmap? {
        // Stub implementation - returns null for MVP
        return null
    }
    
    var cameraPreViewCloseListener: (() -> Unit)? = null
}