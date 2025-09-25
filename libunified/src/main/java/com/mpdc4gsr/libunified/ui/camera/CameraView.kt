package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View

class CameraView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun openCamera() {
        // Basic implementation for MVP - real implementation would open camera
    }
}