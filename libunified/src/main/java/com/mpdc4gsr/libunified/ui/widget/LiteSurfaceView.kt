package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.SurfaceView

class LiteSurfaceView : SurfaceView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    fun scaleBitmap(bitmap: Bitmap?): Bitmap? {
        return bitmap
    }
    
    fun getBitmap(): Bitmap? {
        return null
    }
    
    val width: Int get() = super.getWidth()
    val height: Int get() = super.getHeight()
}