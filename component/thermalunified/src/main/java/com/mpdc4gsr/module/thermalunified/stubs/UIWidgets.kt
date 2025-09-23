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
        tempMode: Int
    )
    
    fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
        // Default implementation
    }
    
    fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
        // Default implementation
    }
}

class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        const val TEMP_MODE_CLOSE = 0
        const val TEMP_MODE_MIN = 1
        const val TEMP_MODE_MAX = 2
        const val TEMP_MODE_INTERVAL = 3
    }
    
    var leftSeekBar = SeekBarStub()
    var rightSeekBar = SeekBarStub()
    var tempMode = TEMP_MODE_CLOSE
    var isEnabled: Boolean = true
    
    private var onRangeChangedListener: OnRangeChangedListener? = null
    var currentRange = floatArrayOf(0f, 100f)
    
    fun setOnRangeChangedListener(listener: OnRangeChangedListener?) {
        onRangeChangedListener = listener
    }
    
    fun setRange(min: Float, max: Float) {
        currentRange[0] = min
        currentRange[1] = max
    }
    
    fun setPseudocode(pseudocode: Int) {
        // Stub implementation
    }
    
    fun setRangeAndPro(minValue: Float, maxValue: Float, leftValue: Float, rightValue: Float) {
        // Stub implementation
    }
    
    fun setColorList(colorList: IntArray?) {
        // Stub implementation
    }
    
    fun setPlaces(places: FloatArray?) {
        // Stub implementation
    }
    
    fun setIndicatorTextDecimalFormat(format: String) {
        // Stub implementation
    }
}

class SeekBarStub {
    var indicatorBackgroundColor: Int = 0
}

class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    fun getBitmap(): android.graphics.Bitmap? = null
    
    fun closeCamera() {
        // Stub implementation
    }
    
    fun openCamera() {
        // Stub implementation
    }
    
    fun setCameraAlpha(alpha: Float) {
        // Stub implementation
    }
    
    fun setRotation(rotation: Boolean) {
        // Stub implementation - convert Boolean to rotation
        super.setRotation(if (rotation) 90f else 0f)
    }
}