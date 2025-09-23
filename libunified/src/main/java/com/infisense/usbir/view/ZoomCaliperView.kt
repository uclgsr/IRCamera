package com.infisense.usbir.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Stub implementation for ZoomCaliperView to enable compilation
 * This is a minimal implementation for MVP - replace with actual implementation when available
 */
class ZoomCaliperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    var bitmap: Bitmap? = null
    var isOpenAmplify: Boolean = false
    
    fun setTextSize(size: Float) {
        // Stub implementation
    }
    
    fun setLinePaintColor(color: Int) {
        // Stub implementation
    }
    
    var listener: ((Float, Float) -> Unit)? = null
    
    fun setOnTrendChangeListener(listener: (Float, Float) -> Unit) {
        this.listener = listener
    }
    
    fun setOnTrendAddListener(listener: () -> Unit) {
        // Stub implementation
    }
    
    fun setOnTrendRemoveListener(listener: () -> Unit) {
        // Stub implementation
    }
    
    var temperatureRegionMode: Int = 0
    var isUserHighTemp: Boolean = false
    var isUserLowTemp: Boolean = false
    
    fun setUserHighTemp(temp: Float) {
        // Stub implementation
    }
    
    fun setUserLowTemp(temp: Float) {
        // Stub implementation
    }
    
    fun updateSelectBitmap() {
        // Stub implementation
    }
    
    fun updateTargetBitmap() {
        // Stub implementation
    }
    
    var isShowFull: Boolean = false
    
    fun setShowCross(show: Boolean) {
        // Stub implementation
    }
    
    fun setRotation(rotation: Float) {
        super.setRotation(rotation)
    }
    
    var regionAndValueBitmap: Bitmap? = null
}