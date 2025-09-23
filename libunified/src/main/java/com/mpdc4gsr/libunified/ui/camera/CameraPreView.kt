package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View

class CameraPreView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    // Additional methods needed for compilation
    var cameraPreViewCloseListener: (() -> Unit)? = null
    
    fun getBitmap(): android.graphics.Bitmap? = null
    fun closeCamera() {}
    fun openCamera() {}
    fun setRotation(enabled: Boolean) {}
    fun setCameraAlpha(alpha: Float) {}
}