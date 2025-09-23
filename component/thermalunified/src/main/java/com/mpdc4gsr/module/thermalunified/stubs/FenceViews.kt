package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Stub implementations for Fence UI components
 * These are placeholders to resolve compilation issues
 */

class FenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var listener: FenceListener? = null
    
    interface FenceListener {
        fun onFenceChanged()
    }
}

class FencePointView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var listener: FencePointListener? = null
    
    interface FencePointListener {
        fun onPointChanged()
    }
}

class FenceLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var listener: FenceLineListener? = null
    
    interface FenceLineListener {
        fun onLineChanged()
    }
}