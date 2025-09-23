package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CameraPreView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    // Properties for camera functionality
    var cameraPreViewCloseListener: (() -> Unit)? = null
    private var isOpen = false
    private var rotationEnabled = false
    private var cameraAlpha = 1.0f
    
    fun getBitmap(): android.graphics.Bitmap? {
        return try {
            val bitmap = android.graphics.Bitmap.createBitmap(
                width.coerceAtLeast(1), 
                height.coerceAtLeast(1), 
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    fun closeCamera() {
        isOpen = false
        cameraPreViewCloseListener?.invoke()
        invalidate()
    }
    
    fun openCamera() {
        isOpen = true
        invalidate()
    }
    
    fun setRotation(enabled: Boolean) {
        rotationEnabled = enabled
        rotation = if (enabled) 90f else 0f
    }
    
    fun setCameraAlpha(alpha: Float) {
        cameraAlpha = alpha.coerceIn(0f, 1f)
        setAlpha(cameraAlpha)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (isOpen) {
            // Draw a simple camera preview indicator
            val paint = Paint().apply {
                color = Color.DKGRAY
                alpha = (255 * cameraAlpha).toInt()
            }
            
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Draw a simple camera icon in the center
            paint.color = Color.WHITE
            val centerX = width / 2f
            val centerY = height / 2f
            canvas.drawCircle(centerX, centerY, 20f, paint)
            
            paint.color = Color.GRAY
            canvas.drawCircle(centerX, centerY, 15f, paint)
        }
    }
}