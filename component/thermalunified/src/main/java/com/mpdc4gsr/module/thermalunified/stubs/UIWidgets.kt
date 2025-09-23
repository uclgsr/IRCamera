package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Stub implementations for missing UI widgets
 * These are placeholders to resolve compilation issues
 */

class CountDownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    fun setCountdownTime(time: Int) {
        // Stub implementation
    }
}

interface OnRangeChangedListener {
    fun onRangeChanged(
        view: RangeSeekBar?, 
        leftValue: Float, 
        rightValue: Float, 
        isFromUser: Boolean, 
        tempMode: Boolean
    )
}

class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var onRangeChangedListener: OnRangeChangedListener? = null
    var currentRange = floatArrayOf(0f, 100f)
    
    fun setRange(min: Float, max: Float) {
        currentRange[0] = min
        currentRange[1] = max
    }
}

class BitmapConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr)

class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    fun getBitmap(): android.graphics.Bitmap? = null
}