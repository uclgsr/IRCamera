package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import com.mpdc4gsr.libunified.R

class Comm3DSeekBar : SeekBar {
    
    constructor(context: Context) : super(context)
    
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(context, attrs)
    }
    
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initAttributes(context, attrs)
    }
    
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CommSeekBar)
            
            // Apply 3D-specific attributes
            val orientation = typedArray.getInt(R.styleable.CommSeekBar_android_orientation, 0)
            val maxWidth = typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxWidth, -1)
            val maxHeight = typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxHeight, -1)
            val minWidth = typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minWidth, -1)
            val minHeight = typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minHeight, -1)
            
            typedArray.recycle()
        }
    }
}