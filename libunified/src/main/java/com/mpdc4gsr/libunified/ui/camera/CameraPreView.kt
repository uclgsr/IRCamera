package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.drawToBitmap
import com.mpdc4gsr.libunified.app.listener.BitmapViewListener

class CameraPreView : View, BitmapViewListener {
    // This listener was defined outside the conflict and is kept
    var cameraPreViewCloseListener: (() -> Unit)? = null

    // Properties from the 'copilot' branch to manage UI state
    private var isOpen = false
    private var cameraAlpha = 1.0f

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    // Use the modern KTX extension function from the 'dev' branch for conciseness
    override fun getBitmap(): Bitmap? {
        return try {
            drawToBitmap()
        } catch (e: Exception) {
            null
        }
    }

    // Use the implemented logic from the 'copilot' branch
    fun openCamera() {
        isOpen = true
        invalidate() // Trigger a redraw
    }

    // Use the implemented logic from the 'copilot' branch
    fun closeCamera() {
        isOpen = false
        cameraPreViewCloseListener?.invoke()
        invalidate() // Trigger a redraw
    }

    // Use the safer implementation from 'copilot' with value coercion
    fun setCameraAlpha(alpha: Float) {
        cameraAlpha = alpha.coerceIn(0f, 1f)
        this.alpha = cameraAlpha
    }

    // Combine logic: create a clear method for boolean-based rotation
    // Use 90f from 'copilot' as it's more common for camera orientation
    fun setRotation(enabled: Boolean) {
        rotation = if (enabled) 90f else 0f
    }

    // Keep the onDraw method from 'copilot' as it handles the visual state
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

    // Keep the BitmapViewListener implementation from the 'dev' branch
    override val viewX: Float get() = x
    override val viewY: Float get() = y
    override val viewAlpha: Float get() = alpha
    override val viewWidth: Float get() = width.toFloat()
    override val viewHeight: Float get() = height.toFloat()
    override val viewScale: Float get() = scaleX
}