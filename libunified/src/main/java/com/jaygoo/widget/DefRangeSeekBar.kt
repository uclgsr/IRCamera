package com.jaygoo.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Stub implementation for DefRangeSeekBar to enable compilation
 * This is a minimal implementation for MVP - replace with actual library when available
 */
class DefRangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    fun setOnRangeChangedListener(listener: OnRangeChangedListener?) {
        // Stub implementation
    }
    
    fun setProgress(progress: Float) {
        // Stub implementation
    }
    
    fun setProgress(leftProgress: Float, rightProgress: Float) {
        // Stub implementation
    }
}

interface OnRangeChangedListener {
    fun onRangeChanged(view: DefRangeSeekBar?, leftValue: Float, rightValue: Float, isFromUser: Boolean)
    fun onStartTrackingTouch(view: DefRangeSeekBar?, isLeft: Boolean)
    fun onStopTrackingTouch(view: DefRangeSeekBar?, isLeft: Boolean)
}
